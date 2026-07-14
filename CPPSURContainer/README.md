# Plataforma de microservicios CPP

Este directorio contiene la migracion progresiva del backend monolitico hacia
microservicios. El primer bloque implementa los servicios de infraestructura:

- `CPPMSConfig`: configuracion centralizada, puerto `8888`.
- `CPPMSEureka`: registro y descubrimiento, puerto `8761`.
- `CPPMSGateway`: puerta de entrada HTTP, puerto interno `8080` y puerto local `8086`.

## Versiones base

- Java 17
- Spring Boot 3.5.x
- Spring Cloud 2025.0.x

## Compilar y probar

Desde `CPPSURContainer`:

```bash
mvn clean verify
```

## Ejecucion manual

Los servicios se inician en este orden:

```bash
mvn -pl CPPMSConfig spring-boot:run
mvn -pl CPPMSEureka spring-boot:run
mvn -pl CPPMSGateway spring-boot:run
```

Comprobaciones:

```text
http://localhost:8888/cpp-api-gateway/default
http://localhost:8761
http://localhost:8080/actuator/health  (Gateway ejecutado manualmente)
http://localhost:8086/actuator/health  (Gateway publicado por Docker Compose)
```

El Gateway escucha internamente en `8080`. Docker Compose publica ese puerto
como `127.0.0.1:8086` para que pueda convivir con el backend monolitico, que
continua escuchando en `8080` mientras funciona como fachada JWT.

Despues de ejecutar `mvn clean verify`, la integracion de los tres procesos se
puede validar con:

```bash
./scripts/verify-infrastructure.sh
```

El Config Server usa temporalmente un repositorio `native` incluido en su
classpath. Esta opcion mantiene el primer entorno reproducible; posteriormente
puede sustituirse por un repositorio Git.

## Identidad durante la migracion

Identidad todavia no es un microservicio independiente. La autenticacion, el
perfil y la administracion de usuarios permanecen en el backend principal bajo
`/api/auth/**` y `/api/admin/**`, accesible localmente por el puerto `8080`.
Por este motivo el Gateway no declara una ruta `cpp-identity-service`: ese
servicio no existe y mantener la ruta produciria una respuesta `503`.

El Gateway Docker, publicado en `8086`, enruta exclusivamente los cinco
dominios ya extraidos: Pacientes, Agenda, Facturacion, Clinico y Turnos. Cuando
se ejecuta el sistema completo, Angular consume la fachada JWT en `8080` y esta
reenvia las operaciones de negocio a los microservicios usando la clave interna
de desarrollo.

## Docker

La infraestructura se puede construir y levantar desde este directorio:

```bash
docker compose -f compose.infrastructure.yaml up --build -d --wait
```

Para consultar su estado:

```bash
docker compose -f compose.infrastructure.yaml ps
```

Para detenerla sin eliminar imagenes:

```bash
docker compose -f compose.infrastructure.yaml down
```

La comprobacion automatizada construye, levanta, valida y finalmente detiene
los tres contenedores:

```bash
./scripts/verify-docker-infrastructure.sh
```

## Primer microservicio de negocio: pacientes

`CPPMSPatient` extrae del monolito el alta y la consulta de pacientes. El
servicio usa una base MySQL exclusiva y Flyway crea su esquema al iniciar.
Durante esta etapa de migracion, las operaciones originales permanecen en el
monolito; el nuevo contrato se publica bajo `/api/patients/**`.

Para levantar infraestructura, MySQL y el servicio de pacientes:

```bash
docker compose \
  -f compose.infrastructure.yaml \
  -f compose.patient.yaml \
  up --build -d --wait
```

MySQL queda disponible en el puerto local `3307` y el servicio en `8081`,
aunque las solicitudes de negocio deben entrar por Gateway en el puerto
local `8086`.

La verificacion integral crea un entorno temporal, comprueba Config Server,
el registro en Eureka, la ruta de Gateway y la persistencia en MySQL:

```bash
./scripts/verify-patient-service.sh
```

Las credenciales incluidas en Compose son exclusivamente para desarrollo
local. En despliegues reales se sustituiran por secretos del orquestador.

## Segundo microservicio de negocio: agenda y citas

`CPPMSScheduling` administra especialidades, psicologos, horarios y citas en
una base PostgreSQL exclusiva. Al reservar, valida el identificador contra
`CPPMSPatient` mediante Eureka y conserva una instantanea minima del paciente;
no existe ninguna clave foranea entre PostgreSQL y MySQL.

Para levantar los dos servicios de negocio y sus bases:

```bash
docker compose \
  -f compose.infrastructure.yaml \
  -f compose.patient.yaml \
  -f compose.scheduling.yaml \
  up --build -d --wait
```

PostgreSQL se publica localmente en `5433` y agenda en `8082`. El contrato
publico entra por Gateway bajo `/api/scheduling/**`.

La prueba integral crea un paciente, obtiene el catalogo, agenda una cita y
comprueba que PostgreSQL rechace una segunda reserva para la misma franja:

```bash
./scripts/verify-scheduling-service.sh
```

## Tercer microservicio de negocio: facturacion

`CPPMSBilling` extrae las deudas, pagos y comprobantes del monolito. Utiliza
SQL Server como base exclusiva y obtiene los datos de una cita consultando a
`CPPMSScheduling` por Eureka. Al registrar un pago solicita a Agenda cambiar
la cita a `PAGADA`; nunca escribe directamente en PostgreSQL.

Para levantar los tres servicios de negocio y sus bases:

```bash
docker compose \
  -f compose.infrastructure.yaml \
  -f compose.patient.yaml \
  -f compose.scheduling.yaml \
  -f compose.billing.yaml \
  up --build -d --wait
```

SQL Server se publica localmente en `1434` y Facturacion en `8083`. La API
publica se encuentra bajo `/api/billing/**` en Gateway.

La prueba integral recorre MySQL, PostgreSQL y SQL Server: crea un paciente,
agenda una cita, genera su deuda, registra el pago, emite el comprobante y
comprueba que Agenda reciba el estado `PAGADA`:

```bash
./scripts/verify-billing-service.sh
```

SQL Server necesita mas memoria que MySQL y PostgreSQL; para la prueba local
el contenedor tiene un limite de 2 GB. Las credenciales de Compose son solo
para desarrollo y deben convertirse en secretos al desplegar Kubernetes.

## Cuarto microservicio de negocio: clinico

`CPPMSClinical` administra procesos terapeuticos, entrevistas iniciales y
sesiones inmutables en una base PostgreSQL exclusiva. Conserva identificadores
y snapshots, valida pacientes y citas mediante Eureka y solicita a Agenda el
cambio `EN_CONSULTA` a `ATENDIDA` tras registrar una sesion.

Se publica bajo `/api/clinical/**`, usa el puerto `8084` y PostgreSQL se expone
localmente en `5434`. La verificacion integral se ejecuta con:

```bash
./scripts/verify-clinical-service.sh
```

## Quinto microservicio de negocio: turnos y tickets

`CPPMSQueue` administra la emisión diaria y la cola de atención en una base
MySQL exclusiva. Una fila de secuencia por fecha se bloquea durante la emisión
y las transiciones, garantizando correlativos únicos y un solo ticket en
atención aun con solicitudes concurrentes.

La API se publica bajo `/api/queue/**`, el servicio usa `8085` y MySQL se
expone localmente en `3308`. La primera entrega usa REST como fuente de verdad;
las notificaciones WebSocket quedan previstas para un cambio posterior.

```bash
./scripts/verify-queue-service.sh
```

## Kubernetes local con Kind

La plataforma completa también dispone de manifiestos Kubernetes en `k8s/`.
El entorno local usa Kind y publica el Gateway en `http://localhost:8086` y la
aplicación Angular en `http://localhost:4200`. Config Server, Eureka, la fachada
JWT, los servicios de negocio y las bases de datos se comunican mediante DNS de
Kubernetes.

Requisitos locales:

- Docker activo.
- `kind`.
- `kubectl`.
- `curl`, `rg` y `jq`.
- Al menos 10 GB de memoria disponibles para Docker, porque la aplicación
  completa ejecuta las bases SQL Server de identidad y facturación.

Desde `CPPSURContainer`, el despliegue completo se ejecuta con:

```bash
./scripts/kind-deploy.sh
```

El script construye las diez imágenes de la aplicación y tres envoltorios
locales de las imágenes oficiales de MySQL, PostgreSQL y SQL Server. Luego crea
el clúster `cpp-local`, carga las imágenes sin necesitar un registro remoto,
inicializa la base de identidad, aplica los manifiestos en orden y espera que
los dieciséis `Deployment` estén
disponibles. Los envoltorios no modifican los motores; sólo generan imágenes
locales de una única plataforma que Kind puede importar de forma reproducible.

La comprobación posterior valida Eureka, una ruta de cada microservicio, el
frontend, el rechazo de solicitudes sin JWT, el login y una consulta autenticada
que recorre Angular, Nginx, la fachada y Pacientes:

```bash
./scripts/kind-verify.sh
```

Para inspeccionar manualmente los recursos:

```bash
kubectl --context kind-cpp-local -n cpp get pods,services,pvc
```

Para eliminar el clúster y sus datos persistentes locales:

```bash
./scripts/kind-delete.sh
```

Los valores de `k8s/base/config.yaml` son credenciales exclusivas para la
demostración local. Antes de una puesta real en producción deben reemplazarse
por secretos externos o por el gestor de secretos de la plataforma.

## Despliegue mínimo en Azure con LoadBalancer

La ruta mínima para la entrega mantiene las bases de datos dentro del clúster
Kubernetes y publica solo dos puntos HTTP mediante `LoadBalancer`:

- `frontend`: URL principal para abrir Angular.
- `api-gateway`: URL técnica para demostrar las rutas de microservicios.

Las bases de datos no se publican a internet. MySQL, PostgreSQL y SQL Server se
ejecutan como `Deployment` internos con `PersistentVolumeClaim`, igual que en
Kind. Esta ruta sirve para exposición académica; para producción se deben usar
bases gestionadas y secretos reales.

Requisitos:

- Azure CLI autenticado con `az login`.
- Un Azure Container Registry.
- Un clúster AKS conectado al ACR.
- `kubectl` apuntando al clúster AKS.
- Docker activo.

Ejemplo de variables:

```bash
export ACR_NAME=cppregistry
export ACR_LOGIN_SERVER=cppregistry.azurecr.io
```

Para construir y subir las diez imágenes propias al ACR:

```bash
./scripts/azure-build-push.sh
```

Para desplegar la plataforma completa en AKS:

```bash
./scripts/azure-deploy-minimal.sh
```

Cuando Azure haya asignado IP pública a los servicios `LoadBalancer`, la
verificación muestra las URLs que van en el informe:

```bash
./scripts/azure-verify-minimal.sh
```

Las URLs resultantes tendrán esta forma:

```text
Frontend: http://<IP_PUBLICA_FRONTEND>/
Gateway:  http://<IP_PUBLICA_GATEWAY>:8080/
```

En el índice del informe, `URL de los Microservicios Desplegados en la nube`
puede documentarse con la URL del Gateway y una tabla de rutas:

```text
GET /api/patients/search?q=87654321
GET /api/scheduling/specialties
GET /api/billing/debts
GET /api/clinical/diagnoses/cie10?q=
GET /api/queue/public/display
```
