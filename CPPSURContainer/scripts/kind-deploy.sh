#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLUSTER_NAME="${KIND_CLUSTER_NAME:-cpp-local}"
KUBE_CONTEXT="kind-${CLUSTER_NAME}"
NAMESPACE="cpp"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Falta el comando requerido: %s\n' "$1" >&2
    exit 1
  fi
}

for command_name in docker kind kubectl rg curl; do
  require_command "$command_name"
done

compose_args=(
  --file "$ROOT_DIR/compose.infrastructure.yaml"
  --file "$ROOT_DIR/compose.patient.yaml"
  --file "$ROOT_DIR/compose.scheduling.yaml"
  --file "$ROOT_DIR/compose.billing.yaml"
  --file "$ROOT_DIR/compose.clinical.yaml"
  --file "$ROOT_DIR/compose.queue.yaml"
)

images=(
  cpp/config-server:0.1.0
  cpp/eureka-server:0.1.0
  cpp/api-gateway:0.1.0
  cpp/patient-service:0.1.0
  cpp/scheduling-service:0.1.0
  cpp/billing-service:0.1.0
  cpp/clinical-service:0.1.0
  cpp/queue-service:0.1.0
  cpp/mysql-kind:8.4
  cpp/postgres-kind:17
  cpp/sqlserver-kind:2022
)

if [[ "${KIND_SKIP_BUILD:-false}" != "true" ]]; then
  printf 'Construyendo imágenes de la plataforma y bases para Kind...\n'
  docker compose "${compose_args[@]}" build
  docker build --platform linux/amd64 --tag cpp/mysql-kind:8.4 \
    --file "$ROOT_DIR/k8s/images/mysql.Dockerfile" "$ROOT_DIR"
  docker build --platform linux/amd64 --tag cpp/postgres-kind:17 \
    --file "$ROOT_DIR/k8s/images/postgres.Dockerfile" "$ROOT_DIR"
  docker build --platform linux/amd64 --tag cpp/sqlserver-kind:2022 \
    --file "$ROOT_DIR/k8s/images/sqlserver.Dockerfile" "$ROOT_DIR"
fi

if ! kind get clusters | rg --quiet --fixed-strings --line-regexp "$CLUSTER_NAME"; then
  printf 'Creando clúster Kind %s...\n' "$CLUSTER_NAME"
  kind create cluster \
    --name "$CLUSTER_NAME" \
    --config "$ROOT_DIR/k8s/kind-config.yaml"
fi

printf 'Cargando imágenes locales en Kind...\n'
for image in "${images[@]}"; do
  docker image inspect "$image" >/dev/null
  kind load docker-image --name "$CLUSTER_NAME" "$image"
done

apply_file() {
  kubectl --context "$KUBE_CONTEXT" apply -f "$1"
}

wait_deployment() {
  kubectl --context "$KUBE_CONTEXT" \
    --namespace "$NAMESPACE" \
    rollout status "deployment/$1" \
    --timeout="${2:-420s}"
}

apply_file "$ROOT_DIR/k8s/base/namespace.yaml"
apply_file "$ROOT_DIR/k8s/base/config.yaml"
apply_file "$ROOT_DIR/k8s/base/databases.yaml"

for deployment in patient-db scheduling-db billing-db clinical-db queue-db; do
  # La primera ejecución puede descargar SQL Server, MySQL y PostgreSQL.
  wait_deployment "$deployment" 900s
done

apply_file "$ROOT_DIR/k8s/base/infrastructure.yaml"
wait_deployment config-server 300s
wait_deployment eureka-server 300s

apply_file "$ROOT_DIR/k8s/base/business-services.yaml"
for deployment in patient-service scheduling-service billing-service clinical-service queue-service; do
  wait_deployment "$deployment" 600s
done

wait_deployment api-gateway 300s

printf 'Esperando el Gateway publicado por Kind...\n'
for _ in {1..60}; do
  if curl --fail --silent http://localhost:8086/actuator/health >/dev/null; then
    break
  fi
  sleep 2
done
curl --fail --silent http://localhost:8086/actuator/health >/dev/null

kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" get pods
printf '\nDespliegue Kind disponible en http://localhost:8086\n'
printf 'Ejecuta ./scripts/kind-verify.sh para validar todos los servicios.\n'
