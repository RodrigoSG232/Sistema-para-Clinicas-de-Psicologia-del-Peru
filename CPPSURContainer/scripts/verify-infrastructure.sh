#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${TMPDIR:-/tmp}/cpp-infrastructure-verification"
PIDS=()

cleanup() {
  for pid in "${PIDS[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
}

show_log() {
  local service="$1"
  if [[ -f "$LOG_DIR/$service.log" ]]; then
    tail -n 80 "$LOG_DIR/$service.log"
  fi
}

wait_for_url() {
  local service="$1"
  local url="$2"

  for _ in $(seq 1 60); do
    if curl --fail --silent "$url" >/dev/null; then
      return 0
    fi
    sleep 1
  done

  printf 'No se pudo iniciar %s. Ultimas lineas del log:\n' "$service" >&2
  show_log "$service" >&2
  return 1
}

start_service() {
  local service="$1"
  local jar="$2"
  shift 2

  env "$@" java -jar "$ROOT_DIR/$jar" >"$LOG_DIR/$service.log" 2>&1 &
  PIDS+=("$!")
}

trap cleanup EXIT INT TERM
mkdir -p "$LOG_DIR"

start_service config-server \
  CPPMSConfig/target/cpp-config-server-0.1.0-SNAPSHOT.jar
wait_for_url config-server http://localhost:8888/actuator/health

curl --fail --silent http://localhost:8888/cpp-api-gateway/default \
  | jq --exit-status '.name == "cpp-api-gateway"' >/dev/null

start_service eureka-server \
  CPPMSEureka/target/cpp-eureka-server-0.1.0-SNAPSHOT.jar \
  CONFIG_SERVER_URL=http://localhost:8888
wait_for_url eureka-server http://localhost:8761/actuator/health

start_service api-gateway \
  CPPMSGateway/target/cpp-api-gateway-0.1.0-SNAPSHOT.jar \
  CONFIG_SERVER_URL=http://localhost:8888 \
  EUREKA_DEFAULT_ZONE=http://localhost:8761/eureka/
wait_for_url api-gateway http://localhost:8080/actuator/health

curl --fail --silent http://localhost:8080/actuator/gateway/routes \
  | jq --exit-status 'length == 5' >/dev/null

printf 'Infraestructura verificada: Config Server, Eureka Server y API Gateway.\n'
