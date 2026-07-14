#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_NAME="cpp-scheduling-verification"
COMPOSE_ARGS=(
  --project-name "$PROJECT_NAME"
  --file "$ROOT_DIR/compose.infrastructure.yaml"
  --file "$ROOT_DIR/compose.patient.yaml"
  --file "$ROOT_DIR/compose.scheduling.yaml"
)

cleanup() {
  docker compose "${COMPOSE_ARGS[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true
}

trap cleanup EXIT INT TERM

docker compose "${COMPOSE_ARGS[@]}" up \
  --build --detach --wait --wait-timeout 420

curl --fail --silent http://localhost:8888/cpp-scheduling-service/default \
  | jq --exit-status '.name == "cpp-scheduling-service"' >/dev/null

registered=false
for _ in {1..30}; do
  if curl --fail --silent \
      --header 'Accept: application/json' \
      http://localhost:8761/eureka/apps/CPP-SCHEDULING-SERVICE \
      | jq --exit-status '.application.name == "CPP-SCHEDULING-SERVICE"' >/dev/null 2>&1; then
    registered=true
    break
  fi
  sleep 2
done

if [[ "$registered" != "true" ]]; then
  printf 'El servicio de agenda no se registró en Eureka.\n' >&2
  exit 1
fi

gateway_ready=false
for _ in {1..30}; do
  gateway_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
    http://localhost:8086/api/scheduling/specialties)"
  if [[ "$gateway_status" == "200" ]]; then
    gateway_ready=true
    break
  fi
  sleep 2
done

if [[ "$gateway_ready" != "true" ]]; then
  printf 'Gateway no encontró una instancia disponible del servicio de agenda.\n' >&2
  exit 1
fi

patient_response="$(curl --fail --silent --show-error \
  --request POST \
  --header 'Content-Type: application/json' \
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

patient_id="$(jq --raw-output '.id' <<<"$patient_response")"
specialty_id="$(curl --fail --silent http://localhost:8086/api/scheduling/specialties \
  | jq --raw-output '.[] | select(.nombre == "Psicologia Clinica") | .id')"
psychologist_id="$(curl --fail --silent \
  "http://localhost:8086/api/scheduling/psychologists?specialtyId=$specialty_id" \
  | jq --raw-output '.[0].id')"

appointment_payload="$(jq --null-input \
  --argjson patientId "$patient_id" \
  --argjson psychologistId "$psychologist_id" \
  --argjson specialtyId "$specialty_id" \
  '{pacienteId: $patientId, psicologoId: $psychologistId,
    especialidadId: $specialtyId, fecha: "2026-07-13", hora: "10:00"}')"

appointment_result="$(curl --silent --show-error \
  --request POST \
  --header 'Content-Type: application/json' \
  --write-out $'\n%{http_code}' \
  --data "$appointment_payload" \
  http://localhost:8086/api/scheduling/appointments)"
appointment_status="${appointment_result##*$'\n'}"
appointment_response="${appointment_result%$'\n'*}"

if [[ "$appointment_status" != "201" ]]; then
  printf 'La reserva devolvió HTTP %s: %s\n' "$appointment_status" "$appointment_response" >&2
  exit 1
fi

jq --exit-status \
  '.estado == "PENDIENTE_PAGO" and .paciente == "Ana Torres" and
   .pacienteHc == "HC-0001" and .especialidad == "Psicologia Clinica" and .monto == 80.00' \
  <<<"$appointment_response" >/dev/null

duplicate_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
  --request POST \
  --header 'Content-Type: application/json' \
  --data "$appointment_payload" \
  http://localhost:8086/api/scheduling/appointments)"

if [[ "$duplicate_status" != "409" ]]; then
  printf 'La reserva duplicada debía devolver HTTP 409, pero devolvió %s.\n' "$duplicate_status" >&2
  exit 1
fi

curl --fail --silent "http://localhost:8086/api/scheduling/appointments/patient/$patient_id" \
  | jq --exit-status 'length == 1 and .[0].estado == "PENDIENTE_PAGO"' >/dev/null

docker compose "${COMPOSE_ARGS[@]}" ps
printf 'Agenda, PostgreSQL y comunicación con pacientes verificados correctamente.\n'
