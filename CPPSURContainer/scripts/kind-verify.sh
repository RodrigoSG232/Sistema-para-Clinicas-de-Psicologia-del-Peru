#!/usr/bin/env bash
set -euo pipefail

CLUSTER_NAME="${KIND_CLUSTER_NAME:-cpp-local}"
KUBE_CONTEXT="kind-${CLUSTER_NAME}"
NAMESPACE="cpp"

for command_name in kind kubectl curl rg jq; do
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
  patient-db scheduling-db billing-db clinical-db queue-db identity-db
  patient-service scheduling-service billing-service clinical-service queue-service
  backend frontend
)

for deployment in "${deployments[@]}"; do
  kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" \
    wait --for=condition=available "deployment/$deployment" --timeout=30s
done

kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" \
  wait --for=condition=complete job/identity-db-init --timeout=30s

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

frontend_url=http://localhost:4200
port_forward_pid=""
port_forward_log=""

cleanup() {
  if [[ -n "$port_forward_pid" ]]; then
    kill "$port_forward_pid" >/dev/null 2>&1 || true
    wait "$port_forward_pid" >/dev/null 2>&1 || true
  fi
  if [[ -n "$port_forward_log" ]]; then
    rm -f "$port_forward_log"
  fi
}
trap cleanup EXIT INT TERM

if ! curl --fail --silent "$frontend_url/" >/dev/null 2>&1; then
  frontend_url=http://127.0.0.1:14200
  port_forward_log="$(mktemp)"
  kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" \
    port-forward service/frontend 14200:80 >"$port_forward_log" 2>&1 &
  port_forward_pid="$!"
  for _ in {1..30}; do
    curl --fail --silent "$frontend_url/" >/dev/null 2>&1 && break
    sleep 1
  done
fi

frontend_html="$(curl --fail --silent "$frontend_url/")"
rg --quiet --ignore-case '<!doctype html|<app-root' <<<"$frontend_html"
printf 'OK %-14s %s\n' Frontend "$frontend_url/"

unauthenticated_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
  "$frontend_url/api/patients/search?q=87654321")"
if [[ "$unauthenticated_status" != "401" && "$unauthenticated_status" != "403" ]]; then
  printf 'La fachada permitió Pacientes sin JWT: HTTP %s.\n' "$unauthenticated_status" >&2
  exit 1
fi
printf 'OK %-14s HTTP %s sin JWT\n' Seguridad "$unauthenticated_status"

login_response="$(curl --fail --silent --show-error \
  --request POST \
  --header 'Content-Type: application/json' \
  --data '{"username":"admin","password":"123"}' \
  "$frontend_url/api/auth/login")"
token="$(jq --exit-status --raw-output 'select(.rol == "ADMIN") | .token' <<<"$login_response")"
[[ -n "$token" && "$token" != "null" ]]
printf 'OK %-14s usuario admin con JWT\n' Identidad

curl --fail --silent \
  --header "Authorization: Bearer $token" \
  "$frontend_url/api/patients/search?q=87654321" \
  | jq --exit-status 'type == "array"' >/dev/null
printf 'OK %-14s Angular → fachada → Pacientes\n' Integración

kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" get pods,services,pvc
printf '\nKind, frontend, identidad JWT, Gateway y los cinco microservicios fueron verificados.\n'
