#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="cpp"
KUBE_CONTEXT="${KUBE_CONTEXT:-}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Falta el comando requerido: %s\n' "$1" >&2
    exit 1
  fi
}

for command_name in kubectl curl jq rg; do
  require_command "$command_name"
done

kubectl_args=()
if [[ -n "$KUBE_CONTEXT" ]]; then
  kubectl_args+=(--context "$KUBE_CONTEXT")
fi

external_address() {
  local service_name="$1"
  kubectl "${kubectl_args[@]}" --namespace "$NAMESPACE" \
    get service "$service_name" \
    -o jsonpath='{.status.loadBalancer.ingress[0].ip}{.status.loadBalancer.ingress[0].hostname}'
}

wait_external_address() {
  local service_name="$1"
  local address=""
  for _ in {1..90}; do
    address="$(external_address "$service_name")"
    if [[ -n "$address" ]]; then
      printf '%s\n' "$address"
      return 0
    fi
    sleep 10
  done
  printf 'Azure aún no asignó IP pública al Service %s.\n' "$service_name" >&2
  return 1
}

frontend_address="$(wait_external_address frontend)"
gateway_address="$(wait_external_address api-gateway)"
frontend_url="http://$frontend_address"
gateway_url="http://$gateway_address:8080"

curl --fail --silent "$frontend_url/" | rg --quiet --ignore-case '<!doctype html|<app-root'
printf 'OK Frontend público: %s/\n' "$frontend_url"

curl --fail --silent "$gateway_url/actuator/health" >/dev/null
printf 'OK Gateway público: %s/actuator/health\n' "$gateway_url"

unauthenticated_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
  "$frontend_url/api/patients/search?q=87654321")"
if [[ "$unauthenticated_status" != "401" && "$unauthenticated_status" != "403" ]]; then
  printf 'La fachada permitió Pacientes sin JWT: HTTP %s.\n' "$unauthenticated_status" >&2
  exit 1
fi
printf 'OK Seguridad: HTTP %s sin JWT\n' "$unauthenticated_status"

login_response="$(curl --fail --silent --show-error \
  --request POST \
  --header 'Content-Type: application/json' \
  --data '{"username":"admin","password":"123"}' \
  "$frontend_url/api/auth/login")"
token="$(jq --exit-status --raw-output 'select(.rol == "ADMIN") | .token' <<<"$login_response")"
[[ -n "$token" && "$token" != "null" ]]
printf 'OK Identidad: admin autenticado\n'

curl --fail --silent \
  --header "Authorization: Bearer $token" \
  "$frontend_url/api/patients/search?q=87654321" \
  | jq --exit-status 'type == "array"' >/dev/null
printf 'OK Integración: Frontend público -> fachada -> Pacientes\n'

printf '\nURLs para el informe:\n'
printf 'Frontend: %s/\n' "$frontend_url"
printf 'Gateway:  %s/\n' "$gateway_url"
