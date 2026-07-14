#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_NAME="cpp-billing-verification"
COMPOSE_ARGS=(
  --project-name "$PROJECT_NAME"
  --file "$ROOT_DIR/compose.infrastructure.yaml"
  --file "$ROOT_DIR/compose.patient.yaml"
  --file "$ROOT_DIR/compose.scheduling.yaml"
  --file "$ROOT_DIR/compose.billing.yaml"
)

cleanup() {
  exit_status=$?
  if [[ "$exit_status" -ne 0 ]]; then
    printf 'La verificación falló. Últimos logs de Facturación:\n' >&2
    docker compose "${COMPOSE_ARGS[@]}" logs --no-color --tail 140 \
      billing-service >&2 || true
  fi
  docker compose "${COMPOSE_ARGS[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true
}

trap cleanup EXIT INT TERM

docker compose "${COMPOSE_ARGS[@]}" up \
  --build --detach --wait --wait-timeout 600

curl --fail --silent http://localhost:8888/cpp-billing-service/default \
  | jq --exit-status '.name == "cpp-billing-service"' >/dev/null

registered=false
for _ in {1..40}; do
  if curl --fail --silent \
      --header 'Accept: application/json' \
      http://localhost:8761/eureka/apps/CPP-BILLING-SERVICE \
      | jq --exit-status '.application.name == "CPP-BILLING-SERVICE"' >/dev/null 2>&1; then
    registered=true
    break
  fi
  sleep 2
done

if [[ "$registered" != "true" ]]; then
  printf 'El servicio de facturación no se registró en Eureka.\n' >&2
  exit 1
fi

gateway_ready=false
for _ in {1..40}; do
  gateway_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
    http://localhost:8086/api/billing/debts)"
  if [[ "$gateway_status" == "200" ]]; then
    gateway_ready=true
    break
  fi
  sleep 2
done

if [[ "$gateway_ready" != "true" ]]; then
  printf 'Gateway no encontró una instancia disponible del servicio de facturación.\n' >&2
  exit 1
fi

printf '1/5 Creando paciente en MySQL...\n'
patient_response="$(curl --fail-with-body --silent --show-error \
  --request POST \
  --header 'Content-Type: application/json' \
  --data '{
    "dni": "87654321",
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

printf '2/5 Creando cita en PostgreSQL...\n'
appointment_response="$(curl --fail-with-body --silent --show-error \
  --request POST \
  --header 'Content-Type: application/json' \
  --data "$appointment_payload" \
  http://localhost:8086/api/scheduling/appointments)"
appointment_id="$(jq --raw-output '.id' <<<"$appointment_response")"

printf '3/5 Generando deuda en SQL Server desde la cita...\n'
debt_response="$(curl --fail-with-body --silent --show-error \
  --request POST \
  http://localhost:8086/api/billing/debts/from-appointment/"$appointment_id")"
debt_id="$(jq --raw-output '.id' <<<"$debt_response")"

jq --exit-status \
  '.estado == "PENDIENTE" and .pacienteNombre == "Ana Torres" and
   .pacienteHc == "HC-0003" and .especialidad == "Psicologia Clinica" and .monto == 80.00' \
  <<<"$debt_response" >/dev/null

printf '4/5 Registrando pago y actualizando Agenda...\n'
payment_response="$(curl --fail-with-body --silent --show-error \
  --request POST \
  --header 'Content-Type: application/json' \
  --data '{"medioPago":"TARJETA","tipo":"BOLETA","cajero":"caja-local"}' \
  http://localhost:8086/api/billing/payments/"$debt_id")"
receipt_id="$(jq --raw-output '.comprobante.id' <<<"$payment_response")"

jq --exit-status \
  '.numeroComprobante == "B-00001" and .mensaje == "Pago registrado exitosamente" and
   .comprobante.estadoDeuda == "PAGADA" and .comprobante.medioPago == "TARJETA" and
   .comprobante.montoPagado == 80.00' <<<"$payment_response" >/dev/null

curl --fail --silent http://localhost:8086/api/scheduling/appointments/"$appointment_id" \
  | jq --exit-status '.estado == "PAGADA"' >/dev/null

curl --fail --silent http://localhost:8086/api/billing/debts/patient/"$patient_id" \
  | jq --exit-status 'length == 0' >/dev/null

curl --fail --silent http://localhost:8086/api/billing/receipts/"$receipt_id" \
  | jq --exit-status '.numeroComprobante == "B-00001" and .estadoDeuda == "PAGADA"' >/dev/null

duplicate_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
  --request POST \
  --header 'Content-Type: application/json' \
  --data '{"medioPago":"EFECTIVO","tipo":"BOLETA"}' \
  http://localhost:8086/api/billing/payments/"$debt_id")"

if [[ "$duplicate_status" != "409" ]]; then
  printf 'El pago duplicado debía devolver HTTP 409, pero devolvió %s.\n' "$duplicate_status" >&2
  exit 1
fi

printf '5/5 Consultas de comprobante y protección contra duplicados superadas.\n'
docker compose "${COMPOSE_ARGS[@]}" ps
printf 'Facturación, SQL Server y coordinación con Agenda verificados correctamente.\n'
