#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_ROOT="$(cd "$ROOT_DIR/.." && pwd)"
CLUSTER_NAME="${KIND_CLUSTER_NAME:-cpp-local}"
KUBE_CONTEXT="kind-${CLUSTER_NAME}"
NAMESPACE="cpp"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Falta el comando requerido: %s\n' "$1" >&2
    exit 1
  fi
}

for command_name in docker kind kubectl rg curl; do
  require_command "$command_name"
done

compose_args=(
  --file "$ROOT_DIR/compose.infrastructure.yaml"
  --file "$ROOT_DIR/compose.patient.yaml"
  --file "$ROOT_DIR/compose.scheduling.yaml"
  --file "$ROOT_DIR/compose.billing.yaml"
  --file "$ROOT_DIR/compose.clinical.yaml"
  --file "$ROOT_DIR/compose.queue.yaml"
)

images=(
  cpp/config-server:0.1.0
  cpp/eureka-server:0.1.0
  cpp/api-gateway:0.1.0
  cpp/patient-service:0.1.0
  cpp/scheduling-service:0.1.0
  cpp/billing-service:0.1.0
  cpp/clinical-service:0.1.0
  cpp/queue-service:0.1.0
  cpp/identity-facade:0.1.0
  cpp/frontend:0.1.0
  cpp/mysql-kind:8.4
  cpp/postgres-kind:17
  cpp/sqlserver-kind:2022
)

if [[ "${KIND_SKIP_BUILD:-false}" != "true" ]]; then
  printf 'Construyendo imágenes de la plataforma y bases para Kind...\n'
  docker compose "${compose_args[@]}" build
  docker build --platform linux/amd64 --tag cpp/mysql-kind:8.4 \
    --file "$ROOT_DIR/k8s/images/mysql.Dockerfile" "$ROOT_DIR"
  docker build --platform linux/amd64 --tag cpp/postgres-kind:17 \
    --file "$ROOT_DIR/k8s/images/postgres.Dockerfile" "$ROOT_DIR"
  docker build --platform linux/amd64 --tag cpp/sqlserver-kind:2022 \
    --file "$ROOT_DIR/k8s/images/sqlserver.Dockerfile" "$ROOT_DIR"
  docker build --platform linux/amd64 --tag cpp/identity-facade:0.1.0 \
    --file "$PROJECT_ROOT/backend/Dockerfile" "$PROJECT_ROOT/backend"
  docker build --platform linux/amd64 --tag cpp/frontend:0.1.0 \
    --file "$PROJECT_ROOT/frontend/Dockerfile" "$PROJECT_ROOT/frontend"
fi

if ! kind get clusters | rg --quiet --fixed-strings --line-regexp "$CLUSTER_NAME"; then
  printf 'Creando clúster Kind %s...\n' "$CLUSTER_NAME"
  kind create cluster \
    --name "$CLUSTER_NAME" \
    --config "$ROOT_DIR/k8s/kind-config.yaml"
fi

printf 'Cargando imágenes locales en Kind...\n'
for image in "${images[@]}"; do
  docker image inspect "$image" >/dev/null
  kind load docker-image --name "$CLUSTER_NAME" "$image"
done

apply_file() {
  kubectl --context "$KUBE_CONTEXT" apply -f "$1"
}

wait_deployment() {
  kubectl --context "$KUBE_CONTEXT" \
    --namespace "$NAMESPACE" \
    rollout status "deployment/$1" \
    --timeout="${2:-420s}"
}

apply_file "$ROOT_DIR/k8s/base/namespace.yaml"
apply_file "$ROOT_DIR/k8s/base/config.yaml"
apply_file "$ROOT_DIR/k8s/base/databases.yaml"

for deployment in patient-db scheduling-db billing-db clinical-db queue-db; do
  # La primera ejecución puede descargar SQL Server, MySQL y PostgreSQL.
  wait_deployment "$deployment" 900s
done

apply_file "$ROOT_DIR/k8s/base/infrastructure.yaml"
wait_deployment config-server 300s
wait_deployment eureka-server 300s

# El Gateway depende de Config Server para cargar sus rutas. Como Kubernetes
# crea los deployments de infraestructura en paralelo, puede arrancar antes de
# que Config Server esté listo y quedar vivo sin rutas. Lo reiniciamos después
# de confirmar Config/Eureka para forzar una carga limpia de configuración.
kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" \
  rollout restart deployment/api-gateway
wait_deployment api-gateway 300s

apply_file "$ROOT_DIR/k8s/base/business-services.yaml"
for deployment in patient-service scheduling-service billing-service clinical-service queue-service; do
  wait_deployment "$deployment" 600s
done

wait_deployment api-gateway 300s

apply_file "$ROOT_DIR/k8s/base/identity-database.yaml"
wait_deployment identity-db 600s

kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" \
  create configmap identity-db-init-script \
  --from-file="init.sql=$PROJECT_ROOT/database/init-sqlserver.sql" \
  --dry-run=client --output=yaml \
  | kubectl --context "$KUBE_CONTEXT" apply -f -

kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" \
  delete job identity-db-init --ignore-not-found
apply_file "$ROOT_DIR/k8s/base/identity-init-job.yaml"
kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" \
  wait --for=condition=complete job/identity-db-init --timeout=300s

apply_file "$ROOT_DIR/k8s/base/application.yaml"
wait_deployment backend 420s
wait_deployment frontend 300s

printf 'Esperando el Gateway publicado por Kind...\n'
for _ in {1..60}; do
  if curl --fail --silent http://localhost:8086/actuator/health >/dev/null; then
    break
  fi
  sleep 2
done
curl --fail --silent http://localhost:8086/actuator/health >/dev/null

kubectl --context "$KUBE_CONTEXT" --namespace "$NAMESPACE" get pods
printf '\nDespliegue Kind disponible en http://localhost:8086\n'
if curl --fail --silent http://localhost:4200/ >/dev/null 2>&1; then
  printf 'Aplicación completa disponible en http://localhost:4200\n'
else
  printf 'El clúster actual no publica 4200; kind-verify.sh usará port-forward.\n'
  printf 'Los clústeres nuevos creados con kind-config.yaml sí publicarán ese puerto.\n'
fi
printf 'Ejecuta ./scripts/kind-verify.sh para validar todos los servicios.\n'
