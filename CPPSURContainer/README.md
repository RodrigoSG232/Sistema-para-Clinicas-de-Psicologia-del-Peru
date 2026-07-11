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
