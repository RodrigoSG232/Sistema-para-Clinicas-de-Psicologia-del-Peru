#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; PROJECT_NAME="cpp-queue-verification"
COMPOSE_ARGS=(--project-name "$PROJECT_NAME" --file "$ROOT_DIR/compose.infrastructure.yaml" --file "$ROOT_DIR/compose.queue.yaml")
TMP_DIR="$(mktemp -d)"; stage="inicio"
cleanup(){ docker compose "${COMPOSE_ARGS[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true; rm -rf "$TMP_DIR"; }
fail(){ printf 'Fallo en etapa [%s]: %s\n' "$stage" "$1" >&2; docker compose "${COMPOSE_ARGS[@]}" logs --tail 120 queue-service queue-db >&2 || true; exit 1; }
trap cleanup EXIT INT TERM
stage="levantar infraestructura y cola"; docker compose "${COMPOSE_ARGS[@]}" up --build --detach --wait --wait-timeout 420 || fail "servicios no saludables"
stage="comprobar Config"; curl -fsS http://localhost:8888/cpp-queue-service/default | jq -e '.name=="cpp-queue-service"' >/dev/null || fail "configuración ausente"
stage="comprobar Eureka"; registered=false; for _ in {1..30}; do if curl -fsS -H 'Accept: application/json' http://localhost:8761/eureka/apps/CPP-QUEUE-SERVICE | jq -e '.application.name=="CPP-QUEUE-SERVICE"' >/dev/null 2>&1; then registered=true; break; fi; sleep 2; done; [[ "$registered" == true ]] || fail "servicio no registrado"
stage="emitir tres tickets concurrentemente"; pids=(); for n in 1 2 3; do (curl -fsS -X POST http://localhost:8086/api/queue/tickets >"$TMP_DIR/ticket-$n.json") & pids+=("$!"); done; for pid in "${pids[@]}"; do wait "$pid" || fail "emisión concurrente falló"; done
numbers="$(jq -r .number "$TMP_DIR"/ticket-*.json | sort | paste -sd, -)"; [[ "$numbers" == "A-001,A-002,A-003" ]] || fail "numeración obtenida: $numbers"
first_id="$(jq -r 'select(.number=="A-001")|.id' "$TMP_DIR"/ticket-*.json)"; second_id="$(jq -r 'select(.number=="A-002")|.id' "$TMP_DIR"/ticket-*.json)"
stage="llamar primer ticket"; curl -fsS -X PATCH http://localhost:8086/api/queue/tickets/"$first_id"/call | jq -e '.status=="EN_ATENCION"' >/dev/null || fail "no se llamó A-001"
stage="rechazar segundo ticket simultáneo"; status="$(curl -sS -o "$TMP_DIR/conflict.json" -w '%{http_code}' -X PATCH http://localhost:8086/api/queue/tickets/"$second_id"/call)"; [[ "$status" == 409 ]] || fail "segundo llamado devolvió HTTP $status"
stage="verificar pantalla pública"; curl -fsS http://localhost:8086/api/queue/public/display | jq -e '.current.number=="A-001" and (.next|length)==2' >/dev/null || fail "pantalla pública incorrecta"
stage="finalizar y llamar siguiente"; curl -fsS -X PATCH http://localhost:8086/api/queue/tickets/"$first_id"/finish | jq -e '.status=="FINALIZADO"' >/dev/null || fail "no se finalizó A-001"; curl -fsS -X PATCH http://localhost:8086/api/queue/tickets/"$second_id"/call | jq -e '.status=="EN_ATENCION"' >/dev/null || fail "no se llamó A-002"
stage="verificar persistencia"; rows="$(docker compose "${COMPOSE_ARGS[@]}" exec -T queue-db mysql -ucpp_queue -pcpp_queue_dev -Nse 'SELECT COUNT(*) FROM queue_ticket' cpp_queue 2>/dev/null)"; [[ "$rows" == 3 ]] || fail "MySQL contiene $rows tickets"
docker compose "${COMPOSE_ARGS[@]}" ps; printf 'Turnos, concurrencia y persistencia verificados correctamente.\n'
