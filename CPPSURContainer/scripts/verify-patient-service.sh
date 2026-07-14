#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_NAME="cpp-patient-verification"
COMPOSE_ARGS=(
  --project-name "$PROJECT_NAME"
  --file "$ROOT_DIR/compose.infrastructure.yaml"
  --file "$ROOT_DIR/compose.patient.yaml"
)

cleanup() {
  docker compose "${COMPOSE_ARGS[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true
}

trap cleanup EXIT INT TERM

docker compose "${COMPOSE_ARGS[@]}" up \
  --build --detach --wait --wait-timeout 360

curl --fail --silent http://localhost:8888/cpp-patient-service/default \
  | jq --exit-status '.name == "cpp-patient-service"' >/dev/null

registered=false
for _ in {1..30}; do
  if curl --fail --silent \
      --header 'Accept: application/json' \
      http://localhost:8761/eureka/apps/CPP-PATIENT-SERVICE \
      | jq --exit-status '.application.name == "CPP-PATIENT-SERVICE"' >/dev/null 2>&1; then
    registered=true
    break
  fi
  sleep 2
done

if [[ "$registered" != "true" ]]; then
  printf 'El servicio de pacientes no se registró en Eureka.\n' >&2
  exit 1
fi

gateway_ready=false
for _ in {1..30}; do
  gateway_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
    http://localhost:8086/api/patients/0)"
  if [[ "$gateway_status" == "404" ]]; then
    gateway_ready=true
    break
  fi
  sleep 2
done

if [[ "$gateway_ready" != "true" ]]; then
  printf 'Gateway no encontró una instancia disponible del servicio de pacientes.\n' >&2
  exit 1
fi

patient_result="$(curl --silent --show-error \
  --request POST \
  --header 'Content-Type: application/json' \
  --write-out $'\n%{http_code}' \
  --data '{
    "dni": "76543210",
    "nombres": "Ana",
    "apellidos": "Torres",
    "fechaNacimiento": "1994-03-15",
    "sexo": "F",
    "telefono": "987654321",
    "email": "ana.torres@example.com",
    "direccion": "Lima"
  }' \
  http://localhost:8086/api/patients)"

patient_status="${patient_result##*$'\n'}"
patient_response="${patient_result%$'\n'*}"

if [[ "$patient_status" != "201" ]]; then
  printf 'El alta de paciente devolvió HTTP %s: %s\n' \
    "$patient_status" "$patient_response" >&2
  exit 1
fi

jq --exit-status \
  '.dni == "76543210" and .numeroHistoria == "HC-0001" and .nombreCompleto == "Ana Torres"' \
  <<<"$patient_response" >/dev/null

curl --fail --silent http://localhost:8086/api/patients/dni/76543210 \
  | jq --exit-status '.dni == "76543210" and .numeroHistoria == "HC-0001"' >/dev/null

docker compose "${COMPOSE_ARGS[@]}" ps
printf 'Microservicio de pacientes y MySQL verificados correctamente.\n'
