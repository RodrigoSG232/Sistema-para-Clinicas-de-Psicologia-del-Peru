#!/usr/bin/env bash
set -euo pipefail

CLUSTER_NAME="${KIND_CLUSTER_NAME:-cpp-local}"
KUBE_CONTEXT="kind-${CLUSTER_NAME}"
NAMESPACE="cpp"

for command_name in kind kubectl curl rg; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    printf 'Falta el comando requerido: %s\n' "$command_name" >&2
    exit 1
  fi
done

if ! kind get clusters | rg --quiet --fixed-strings --line-regexp "$CLUSTER_NAME"; then
  printf 'El clúster Kind %s no existe. Ejecuta kind-deploy.sh primero.\n' "$CLUSTER_NAME" >&2
  exit 1
fi

deployments=(
  config-server eureka-server api-gateway
  patient-db scheduling-db billing-db clinical-db queue-db
  patient-service scheduling-service billing-service clinical-service queue-service
)

for deployment in "${deployments[@]}"; do
  kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" \
    wait --for=condition=available "deployment/$deployment" --timeout=30s
done

registry="$(kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" \
  exec deployment/eureka-server -- \
  wget --quiet --output-document=- http://localhost:8761/eureka/apps)"

for application in \
  CPP-API-GATEWAY CPP-PATIENT-SERVICE CPP-SCHEDULING-SERVICE \
  CPP-BILLING-SERVICE CPP-CLINICAL-SERVICE CPP-QUEUE-SERVICE; do
  if ! rg --quiet --fixed-strings "$application" <<<"$registry"; then
    printf 'Eureka no contiene la aplicación %s.\n' "$application" >&2
    exit 1
  fi
done

check_route() {
  local path="$1"
  local description="$2"
  for _ in {1..60}; do
    if curl --fail --silent "http://localhost:8086$path" >/dev/null; then
      printf 'OK %-14s %s\n' "$description" "$path"
      return 0
    fi
    sleep 2
  done
  printf 'No respondió la ruta de %s: %s\n' "$description" "$path" >&2
  return 1
}

check_route /actuator/health Gateway
check_route '/api/patients/search?q=87654321' Pacientes
check_route /api/scheduling/specialties Agenda
check_route /api/billing/debts Facturación
check_route '/api/clinical/diagnoses/cie10?q=' Clínica
check_route /api/queue/public/display Turnos

kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" get pods,services,pvc
printf '\nKind, Kubernetes, Eureka, Gateway y los cinco microservicios fueron verificados.\n'

