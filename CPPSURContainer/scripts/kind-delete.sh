#!/usr/bin/env bash
set -euo pipefail

CLUSTER_NAME="${KIND_CLUSTER_NAME:-cpp-local}"

if ! command -v kind >/dev/null 2>&1; then
  printf 'Falta el comando requerido: kind\n' >&2
  exit 1
fi

kind delete cluster --name "$CLUSTER_NAME"
printf 'Clúster Kind %s eliminado. Sus volúmenes locales también fueron descartados.\n' "$CLUSTER_NAME"

