#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_ROOT="$(cd "$ROOT_DIR/.." && pwd)"
ACR_LOGIN_SERVER="${ACR_LOGIN_SERVER:?Define ACR_LOGIN_SERVER. Ejemplo: cppregistry.azurecr.io}"
ACR_NAME="${ACR_NAME:-}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Falta el comando requerido: %s\n' "$1" >&2
    exit 1
  fi
}

for command_name in docker; do
  require_command "$command_name"
done

if [[ -n "$ACR_NAME" ]]; then
  require_command az
  az acr login --name "$ACR_NAME"
fi

compose_args=(
  --file "$ROOT_DIR/compose.infrastructure.yaml"
  --file "$ROOT_DIR/compose.patient.yaml"
  --file "$ROOT_DIR/compose.scheduling.yaml"
  --file "$ROOT_DIR/compose.billing.yaml"
  --file "$ROOT_DIR/compose.clinical.yaml"
  --file "$ROOT_DIR/compose.queue.yaml"
)

app_images=(
  cpp/config-server:0.1.0
  cpp/eureka-server:0.1.0
  cpp/api-gateway:0.1.0
  cpp/patient-service:0.1.0
  cpp/scheduling-service:0.1.0
  cpp/billing-service:0.1.0
  cpp/clinical-service:0.1.0
  cpp/queue-service:0.1.0
)

printf 'Construyendo imágenes backend de microservicios...\n'
docker compose "${compose_args[@]}" build

printf 'Construyendo fachada JWT y frontend...\n'
docker build --platform linux/amd64 --tag cpp/identity-facade:0.1.0 \
  --file "$PROJECT_ROOT/backend/Dockerfile" "$PROJECT_ROOT/backend"
docker build --platform linux/amd64 --tag cpp/frontend:0.1.0 \
  --file "$PROJECT_ROOT/frontend/Dockerfile" "$PROJECT_ROOT/frontend"

app_images+=(cpp/identity-facade:0.1.0 cpp/frontend:0.1.0)

printf 'Etiquetando y subiendo imágenes a %s...\n' "$ACR_LOGIN_SERVER"
for image in "${app_images[@]}"; do
  remote_image="$ACR_LOGIN_SERVER/$image"
  docker tag "$image" "$remote_image"
  docker push "$remote_image"
done

printf '\nImágenes publicadas en ACR. Continúa con azure-deploy-minimal.sh.\n'
