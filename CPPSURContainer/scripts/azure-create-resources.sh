#!/usr/bin/env bash
set -euo pipefail

LOCATION="${AZURE_LOCATION:-eastus}"
RESOURCE_GROUP="${AZURE_RESOURCE_GROUP:-rg-cpp-microservices}"
AKS_NAME="${AKS_NAME:-aks-cpp-microservices}"
ACR_NAME="${ACR_NAME:-}"
NODE_COUNT="${AKS_NODE_COUNT:-1}"
NODE_VM_SIZE="${AKS_NODE_VM_SIZE:-Standard_B4ms}"

if [[ "${CONFIRM_AZURE_COSTS:-false}" != "true" ]]; then
  printf 'Este script crea recursos en Azure que pueden generar costos.\n' >&2
  printf 'Vuelve a ejecutarlo con CONFIRM_AZURE_COSTS=true si deseas continuar.\n' >&2
  exit 1
fi

if [[ -z "$ACR_NAME" ]]; then
  random_suffix="$(date +%s | tail -c 6)"
  ACR_NAME="cppms${random_suffix}"
fi

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Falta el comando requerido: %s\n' "$1" >&2
    exit 1
  fi
}

require_command az
require_command kubectl

printf 'Cuenta activa de Azure:\n'
az account show --query '{subscription:name, tenant:tenantId, user:user.name}' --output table

printf '\nRegistrando proveedores requeridos si fuese necesario...\n'
az provider register --namespace Microsoft.ContainerService --wait
az provider register --namespace Microsoft.ContainerRegistry --wait

printf '\nCreando grupo de recursos %s en %s...\n' "$RESOURCE_GROUP" "$LOCATION"
az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --output table

printf '\nCreando Azure Container Registry %s...\n' "$ACR_NAME"
az acr create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" \
  --sku Basic \
  --output table

acr_login_server="$(az acr show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" \
  --query loginServer \
  --output tsv)"

printf '\nCreando clúster AKS %s conectado al ACR...\n' "$AKS_NAME"
az aks create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$AKS_NAME" \
  --node-count "$NODE_COUNT" \
  --node-vm-size "$NODE_VM_SIZE" \
  --generate-ssh-keys \
  --attach-acr "$ACR_NAME" \
  --output table

printf '\nConfigurando kubectl contra AKS...\n'
az aks get-credentials \
  --resource-group "$RESOURCE_GROUP" \
  --name "$AKS_NAME" \
  --overwrite-existing

kubectl get nodes

cat <<EOF

Recursos creados.

Usa estos valores para el despliegue:

export AZURE_RESOURCE_GROUP=$RESOURCE_GROUP
export AKS_NAME=$AKS_NAME
export ACR_NAME=$ACR_NAME
export ACR_LOGIN_SERVER=$acr_login_server

Siguiente paso:
./scripts/azure-build-push.sh
./scripts/azure-deploy-minimal.sh
./scripts/azure-verify-minimal.sh
EOF
