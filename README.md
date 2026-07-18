# Sistema para Clínicas de Psicología del Perú

Guía paso a paso para levantar el proyecto completo con Docker.

## 1. Requisitos

Antes de iniciar, asegúrate de tener instalado:

- Git.
- Docker.
- Docker Compose.

Verifica que Docker esté funcionando:

```bash
docker info
```

Si el comando muestra información de Docker, puedes continuar.

## 2. Clonar el repositorio

```bash
git clone <URL_DEL_REPOSITORIO>
cd Sistema-para-Clinicas-de-Psicologia-del-Peru
```

## 3. Levantar los microservicios

Primero se levantan los servicios de infraestructura, los microservicios de
negocio y sus bases de datos.

Desde la raíz del proyecto:

```bash
cd CPPSURContainer
```

Ejecuta:

```bash
docker compose \
  -f compose.infrastructure.yaml \
  -f compose.patient.yaml \
  -f compose.scheduling.yaml \
  -f compose.billing.yaml \
  -f compose.clinical.yaml \
  -f compose.queue.yaml \
  up -d --build
```

Este comando levanta:

- Config Server.
- Eureka Server.
- API Gateway.
- Patient Service.
- Scheduling Service.
- Billing Service.
- Clinical Service.
- Queue Service.
- Bases de datos MySQL, PostgreSQL y SQL Server usadas por los microservicios.

## 4. Levantar frontend, backend de autenticación y base de identidad

Abre otra terminal o vuelve a la raíz del proyecto:

```bash
cd ..
```

Desde la raíz ejecuta:

```bash
docker compose up -d --build
```

Este comando levanta:

- Backend principal de autenticación y usuarios.
- Base de datos de identidad.
- Frontend Angular servido con Nginx.

## 5. Verificar que los contenedores estén activos

Ejecuta:

```bash
docker ps
```

Debes ver contenedores relacionados con:

- Config Server.
- Eureka.
- Gateway.
- Patient.
- Scheduling.
- Billing.
- Clinical.
- Queue.
- Backend.
- Frontend.
- Bases de datos.

Si algún contenedor aún no aparece listo, espera uno o dos minutos y vuelve a
ejecutar:

```bash
docker ps
```

## 6. Verificar Eureka

Abre en el navegador:

```text
http://localhost:8761
```

En Eureka deben aparecer registrados los microservicios, por ejemplo:

- CPP-API-GATEWAY.
- CPP-PATIENT-SERVICE.
- CPP-SCHEDULING-SERVICE.
- CPP-BILLING-SERVICE.
- CPP-CLINICAL-SERVICE.
- CPP-QUEUE-SERVICE.

## 7. Verificar el API Gateway

Ejecuta:

```bash
curl http://localhost:8086/actuator/health
```

Debe responder algo similar a:

```json
{"status":"UP"}
```

Si entras directamente a:

```text
http://localhost:8086/
```

puede aparecer una página `404`. Eso es normal, porque el Gateway no tiene una
pantalla principal; funciona mediante rutas `/api/...`.

## 8. Probar endpoints de microservicios

Puedes probar estas rutas desde el navegador o con `curl`:

```text
http://localhost:8086/api/patients/search?q=87654321
```

```text
http://localhost:8086/api/scheduling/specialties
```

```text
http://localhost:8086/api/billing/debts
```

```text
http://localhost:8086/api/clinical/diagnoses/cie10?q=
```

```text
http://localhost:8086/api/queue/public/display
```

## 9. Abrir el frontend

Abre en el navegador:

```text
http://localhost:4200
```

## 10. Usuarios de prueba

Contraseña para los usuarios de prueba:

```text
123
```

Usuarios disponibles:

| Usuario | Rol |
|---|---|
| `admin` | ADMIN |
| `recepcion` | RECEPCION |
| `caja` | CAJA |
| `psicologo` | PSICOLOGO |
| `psicologo2` | PSICOLOGO |
| `anfitriona` | ANFITRIONA |
| `enfermera` | ENFERMERA |

## 11. Apagar el proyecto

Primero apaga los servicios de la raíz del proyecto:

```bash
docker compose down
```

Luego entra a la carpeta de microservicios:

```bash
cd CPPSURContainer
```

Y apaga los microservicios:

```bash
docker compose \
  -f compose.infrastructure.yaml \
  -f compose.patient.yaml \
  -f compose.scheduling.yaml \
  -f compose.billing.yaml \
  -f compose.clinical.yaml \
  -f compose.queue.yaml \
  down
```

## 12. Reiniciar el proyecto

Si ya apagaste el proyecto y quieres levantarlo otra vez, repite estos pasos:

```bash
cd CPPSURContainer
docker compose \
  -f compose.infrastructure.yaml \
  -f compose.patient.yaml \
  -f compose.scheduling.yaml \
  -f compose.billing.yaml \
  -f compose.clinical.yaml \
  -f compose.queue.yaml \
  up -d --build
```

Luego:

```bash
cd ..
docker compose up -d --build
```

Finalmente abre:

```text
http://localhost:4200
```

## 13. Levantar con Kubernetes local usando Kind

El proyecto también puede levantarse localmente con Kubernetes usando Kind.
Antes de usar Kind, apaga los contenedores de Docker Compose para evitar cruce
de puertos como `4200`, `8761` y `8086`.

Desde la raíz del proyecto:

```bash
docker compose down
```

Luego apaga los microservicios:

```bash
cd CPPSURContainer
docker compose \
  -f compose.infrastructure.yaml \
  -f compose.patient.yaml \
  -f compose.scheduling.yaml \
  -f compose.billing.yaml \
  -f compose.clinical.yaml \
  -f compose.queue.yaml \
  down
```

Para ejecutar Kind se necesita:

- Docker activo.
- `kind`.
- `kubectl`.
- `curl`.
- `jq`.
- `ripgrep` (`rg`).

En Linux, instala las herramientas necesarias según tu distribución. En Ubuntu
o Debian:

```bash
sudo apt update
sudo apt install curl jq ripgrep -y
```

Luego verifica:

```bash
docker info
kind --version
kubectl version --client
rg --version
jq --version
```

Ejecuta el despliegue:

```bash
./scripts/kind-deploy.sh
```

Verifica:

```bash
./scripts/kind-verify.sh
```

Para eliminar el clúster local:

```bash
./scripts/kind-delete.sh
```

### Nota para usuarios de Windows

En Windows se recomienda ejecutar Kind desde WSL2, no desde PowerShell ni desde
Git Bash.

Pasos recomendados:

1. Instalar Docker Desktop.
2. Instalar WSL2.
3. Instalar Ubuntu desde Microsoft Store.
4. Abrir Docker Desktop.
5. Ir a `Settings > Resources > WSL Integration`.
6. Activar la integración con Ubuntu.
7. Aplicar cambios y reiniciar Docker Desktop.

Dentro de Ubuntu/WSL, verifica Docker:

```bash
docker info
```

Instala herramientas:

```bash
sudo apt update
sudo apt install curl jq ripgrep dos2unix -y
```

Instala `kubectl`:

```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
```

Instala `kind`:

```bash
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.29.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind
```

Clona el repositorio dentro del sistema de archivos de WSL, por ejemplo en
`/home/tu_usuario/`, no dentro de `/mnt/c/`.

Luego entra al proyecto:

```bash
cd ~/Sistema-para-Clinicas-de-Psicologia-del-Peru/CPPSURContainer
```

Si los scripts no tienen permiso de ejecución:

```bash
chmod +x scripts/kind-deploy.sh scripts/kind-verify.sh scripts/kind-delete.sh
```

Si los scripts tienen saltos de línea de Windows:

```bash
dos2unix scripts/kind-deploy.sh scripts/kind-verify.sh scripts/kind-delete.sh
```

Finalmente ejecuta:

```bash
./scripts/kind-deploy.sh
./scripts/kind-verify.sh
```
