#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_ROOT="$(cd "$ROOT_DIR/.." && pwd)"
NAMESPACE="cpp"
ACR_LOGIN_SERVER="${ACR_LOGIN_SERVER:?Define ACR_LOGIN_SERVER. Ejemplo: cppregistry.azurecr.io}"
KUBE_CONTEXT="${KUBE_CONTEXT:-}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Falta el comando requerido: %s\n' "$1" >&2
    exit 1
  fi
}

for command_name in kubectl sed mktemp; do
  require_command "$command_name"
done

kubectl_args=()
if [[ -n "$KUBE_CONTEXT" ]]; then
  kubectl_args+=(--context "$KUBE_CONTEXT")
fi

overlay_source="$ROOT_DIR/k8s/overlays/azure-loadbalancer"
overlay_rendered="$(mktemp -d "$ROOT_DIR/k8s/overlays/.azure-rendered.XXXXXX")"
cleanup() {
  rm -rf "$overlay_rendered"
}
trap cleanup EXIT INT TERM

cp -R "$overlay_source/." "$overlay_rendered/"
sed -i "s#__ACR_LOGIN_SERVER__#$ACR_LOGIN_SERVER#g" "$overlay_rendered/kustomization.yaml"

printf 'Creando namespace y script de inicialización de identidad...\n'
kubectl "${kubectl_args[@]}" apply -f "$ROOT_DIR/k8s/base/namespace.yaml"
kubectl "${kubectl_args[@]}" --namespace "$NAMESPACE" \
  create configmap identity-db-init-script \
  --from-file="init.sql=$PROJECT_ROOT/database/init-sqlserver.sql" \
  --dry-run=client --output=yaml \
  | kubectl "${kubectl_args[@]}" apply -f -

kubectl "${kubectl_args[@]}" --namespace "$NAMESPACE" \
  delete job identity-db-init --ignore-not-found

printf 'Aplicando manifiestos mínimos para Azure con LoadBalancer...\n'
kubectl "${kubectl_args[@]}" apply -k "$overlay_rendered"

deployments=(
  patient-db scheduling-db billing-db clinical-db queue-db identity-db
  config-server eureka-server api-gateway
  patient-service scheduling-service billing-service clinical-service queue-service
  backend frontend
)

for deployment in "${deployments[@]}"; do
  kubectl "${kubectl_args[@]}" --namespace "$NAMESPACE" \
    rollout status "deployment/$deployment" --timeout=900s
done

kubectl "${kubectl_args[@]}" --namespace "$NAMESPACE" \
  wait --for=condition=complete job/identity-db-init --timeout=420s

kubectl "${kubectl_args[@]}" --namespace "$NAMESPACE" get services
printf '\nDespliegue aplicado. Ejecuta ./scripts/azure-verify-minimal.sh cuando Azure asigne IP pública.\n'
