#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/compose.infrastructure.yaml"

cleanup() {
  docker compose --file "$COMPOSE_FILE" down --remove-orphans >/dev/null 2>&1 || true
}

trap cleanup EXIT INT TERM

docker compose --file "$COMPOSE_FILE" up \
  --build --detach --wait --wait-timeout 240

curl --fail --silent http://localhost:8888/cpp-api-gateway/default \
  | jq --exit-status '.name == "cpp-api-gateway"' >/dev/null

curl --fail --silent http://localhost:8086/actuator/gateway/routes \
  | jq --exit-status 'length == 5' >/dev/null

docker compose --file "$COMPOSE_FILE" ps
printf 'Infraestructura Docker verificada correctamente.\n'
