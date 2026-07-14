#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_NAME="cpp-clinical-verification"
COMPOSE_ARGS=(--project-name "$PROJECT_NAME" --file "$ROOT_DIR/compose.infrastructure.yaml" --file "$ROOT_DIR/compose.patient.yaml" --file "$ROOT_DIR/compose.scheduling.yaml" --file "$ROOT_DIR/compose.billing.yaml" --file "$ROOT_DIR/compose.clinical.yaml")
stage="inicio"
fail(){ printf 'Fallo en etapa [%s]: %s\n' "$stage" "$1" >&2; exit 1; }
cleanup(){ docker compose "${COMPOSE_ARGS[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true; }
trap cleanup EXIT INT TERM

stage="levantar plataforma"; docker compose "${COMPOSE_ARGS[@]}" up --build --detach --wait --wait-timeout 600 || fail "Docker Compose no quedó saludable"
stage="validar Config y Eureka"; curl --fail --silent http://localhost:8888/cpp-clinical-service/default | jq -e '.name=="cpp-clinical-service"' >/dev/null || fail "Config no publica el servicio"
for _ in {1..30}; do curl --fail --silent -H 'Accept: application/json' http://localhost:8761/eureka/apps/CPP-CLINICAL-SERVICE | jq -e '.application.name=="CPP-CLINICAL-SERVICE"' >/dev/null 2>&1 && break; sleep 2; done

stage="crear paciente"; patient="$(curl --fail-with-body -sS -X POST -H 'Content-Type: application/json' --data '{"dni":"76543210","nombres":"Ana","apellidos":"Torres","fechaNacimiento":"1994-03-15","sexo":"F","telefono":"987654321","email":"ana@example.com","direccion":"Lima"}' http://localhost:8086/api/patients)" || fail "No se creó paciente"; patient_id="$(jq -r .id <<<"$patient")"
specialty_id="$(curl -fsS http://localhost:8086/api/scheduling/specialties | jq -r '.[]|select(.nombre=="Psicologia Clinica")|.id')"; psychologist_id="$(curl -fsS "http://localhost:8086/api/scheduling/psychologists?specialtyId=$specialty_id" | jq -r '.[0].id')"
stage="crear cita"; appointment_payload="$(jq -n --argjson p "$patient_id" --argjson psy "$psychologist_id" --argjson sp "$specialty_id" '{pacienteId:$p,psicologoId:$psy,especialidadId:$sp,fecha:"2026-07-13",hora:"10:00"}')"; appointment="$(curl --fail-with-body -sS -X POST -H 'Content-Type: application/json' --data "$appointment_payload" http://localhost:8086/api/scheduling/appointments)" || fail "No se creó cita"; appointment_id="$(jq -r .id <<<"$appointment")"
stage="pagar cita"; debt="$(curl --fail-with-body -sS -X POST http://localhost:8086/api/billing/debts/from-appointment/"$appointment_id")" || fail "No se generó deuda"; debt_id="$(jq -r .id <<<"$debt")"; curl --fail-with-body -sS -X POST -H 'Content-Type: application/json' --data '{"medioPago":"TARJETA","tipo":"BOLETA","cajero":"caja-local"}' http://localhost:8086/api/billing/payments/"$debt_id" >/dev/null || fail "No se pagó la cita"
stage="llevar cita a consulta"; for state in EN_PISO EN_CONSULTA; do curl --fail-with-body -sS -X PATCH -H 'Content-Type: application/json' --data "{\"estado\":\"$state\"}" http://localhost:8086/api/scheduling/appointments/"$appointment_id" >/dev/null || fail "No se cambió a $state"; done
stage="crear proceso e entrevista"; process_payload="$(jq -n --argjson a "$appointment_id" --argjson psy "$psychologist_id" '{appointmentId:$a,psychologistId:$psy,entrevista:{motivoConsulta:"Ansiedad persistente",antecedentesPersonales:"Sin tratamiento previo",observacionesIniciales:"Iniciar evaluación"}}')"; process="$(curl --fail-with-body -sS -X POST -H 'Content-Type: application/json' --data "$process_payload" http://localhost:8086/api/clinical/patients/"$patient_id"/processes)" || fail "No se creó proceso"; process_id="$(jq -r .id <<<"$process")"
stage="registrar sesión"; session_payload="$(jq -n --argjson p "$process_id" --argjson a "$appointment_id" '{processId:$p,appointmentId:$a,evolution:"Entrevista y evaluación inicial",patientIndications:"Registrar emociones",registeredBy:"Dra. Jose Martinez"}')"; session="$(curl --fail-with-body -sS -X POST -H 'Content-Type: application/json' --data "$session_payload" http://localhost:8086/api/clinical/sessions)" || fail "No se registró sesión"; jq -e '.sessionPhase==1' <<<"$session" >/dev/null || fail "Respuesta de sesión inválida"
stage="comprobar persistencia"; rows="$(docker compose "${COMPOSE_ARGS[@]}" exec -T clinical-db psql -U cpp_clinical -d cpp_clinical -tAc "SELECT count(*) FROM clinical_session WHERE appointment_id=$appointment_id")"; [[ "$rows" == "1" ]] || fail "La sesión no está persistida"
stage="comprobar Agenda"; curl -fsS http://localhost:8086/api/scheduling/appointments/"$appointment_id" | jq -e '.estado=="ATENDIDA"' >/dev/null || fail "Agenda no quedó ATENDIDA"
stage="rechazar duplicado"; status="$(curl -sS -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' --data "$session_payload" http://localhost:8086/api/clinical/sessions)"; [[ "$status" == "409" ]] || fail "Duplicado devolvió HTTP $status"
docker compose "${COMPOSE_ARGS[@]}" ps
printf 'Microservicio clínico verificado correctamente.\n'
