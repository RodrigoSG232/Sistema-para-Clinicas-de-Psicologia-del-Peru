#!/usr/bin/env bash
set -euo pipefail

PUBLIC_IP="${1:?Uso: ./scripts/verify-cloud-vm.sh <IP_PUBLICA>}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Falta el comando requerido: %s\n' "$1" >&2
    exit 1
  fi
}

for command_name in curl jq; do
  require_command "$command_name"
done

frontend_url="http://$PUBLIC_IP:4200"
gateway_url="http://$PUBLIC_IP:8086"

curl --fail --silent "$frontend_url/" >/dev/null
printf 'OK Frontend: %s/\n' "$frontend_url"

curl --fail --silent "$gateway_url/actuator/health" >/dev/null
printf 'OK Gateway: %s/actuator/health\n' "$gateway_url"

status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
  "$frontend_url/api/patients/search?q=87654321")"
if [[ "$status" != "401" && "$status" != "403" ]]; then
  printf 'La fachada permitió Pacientes sin JWT: HTTP %s.\n' "$status" >&2
  exit 1
fi
printf 'OK Seguridad: HTTP %s sin JWT\n' "$status"

login_response="$(curl --fail --silent --show-error \
  --request POST \
  --header 'Content-Type: application/json' \
  --data '{"username":"admin","password":"123"}' \
  "$frontend_url/api/auth/login")"
token="$(jq --exit-status --raw-output 'select(.rol == "ADMIN") | .token' <<<"$login_response")"
[[ -n "$token" && "$token" != "null" ]]
printf 'OK Login: admin con JWT\n'

curl --fail --silent \
  --header "Authorization: Bearer $token" \
  "$frontend_url/api/patients/search?q=87654321" \
  | jq --exit-status 'type == "array"' >/dev/null
printf 'OK Integración: frontend público -> backend -> microservicio Pacientes\n'

printf '\nURLs para el informe:\n'
printf 'Frontend: %s/\n' "$frontend_url"
printf 'Gateway:  %s/\n' "$gateway_url"
