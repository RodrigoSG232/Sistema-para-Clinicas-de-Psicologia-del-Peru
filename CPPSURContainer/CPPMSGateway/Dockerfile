FROM maven:3.9.11-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

COPY pom.xml ./
COPY CPPMSConfig/pom.xml CPPMSConfig/pom.xml
COPY CPPMSEureka/pom.xml CPPMSEureka/pom.xml
COPY CPPMSGateway/pom.xml CPPMSGateway/pom.xml
COPY CPPMSPatient/pom.xml CPPMSPatient/pom.xml
COPY CPPMSScheduling/pom.xml CPPMSScheduling/pom.xml
COPY CPPMSBilling/pom.xml CPPMSBilling/pom.xml
COPY CPPMSClinical/pom.xml CPPMSClinical/pom.xml
COPY CPPMSQueue/pom.xml CPPMSQueue/pom.xml

RUN --mount=type=cache,id=cpp-maven-repository,target=/root/.m2,sharing=shared \
    mvn --batch-mode --no-transfer-progress \
    -pl CPPMSGateway -am dependency:go-offline

COPY CPPMSGateway/src CPPMSGateway/src

RUN --mount=type=cache,id=cpp-maven-repository,target=/root/.m2,sharing=shared \
    mvn --batch-mode --no-transfer-progress \
    -pl CPPMSGateway -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="CPP API Gateway" \
      org.opencontainers.image.description="Punto de entrada de los microservicios CPP"

RUN addgroup -S cpp && adduser -S cpp -G cpp

WORKDIR /app
COPY --from=build /workspace/CPPMSGateway/target/cpp-api-gateway-*.jar app.jar

USER cpp
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
