# Plataforma de microservicios CPP

Este directorio contiene la migracion progresiva del backend monolitico hacia
microservicios. El primer bloque implementa los servicios de infraestructura:

- `CPPMSConfig`: configuracion centralizada, puerto `8888`.
- `CPPMSEureka`: registro y descubrimiento, puerto `8761`.
- `CPPMSGateway`: puerta de entrada HTTP, puerto `8080`.

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
http://localhost:8080/actuator/health
```

Despues de ejecutar `mvn clean verify`, la integracion de los tres procesos se
puede validar con:

```bash
./scripts/verify-infrastructure.sh
```

El Config Server usa temporalmente un repositorio `native` incluido en su
classpath. Esta opcion mantiene el primer entorno reproducible; posteriormente
puede sustituirse por un repositorio Git.

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
`8080`.

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
