FROM maven:3.9.11-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

COPY pom.xml ./
COPY CPPMSConfig/pom.xml CPPMSConfig/pom.xml
COPY CPPMSEureka/pom.xml CPPMSEureka/pom.xml
COPY CPPMSGateway/pom.xml CPPMSGateway/pom.xml
COPY CPPMSPatient/pom.xml CPPMSPatient/pom.xml
COPY CPPMSScheduling/pom.xml CPPMSScheduling/pom.xml
COPY CPPMSBilling/pom.xml CPPMSBilling/pom.xml

RUN --mount=type=cache,id=cpp-maven-repository,target=/root/.m2,sharing=shared \
    mvn --batch-mode --no-transfer-progress \
    -pl CPPMSEureka -am dependency:go-offline

COPY CPPMSEureka/src CPPMSEureka/src

RUN --mount=type=cache,id=cpp-maven-repository,target=/root/.m2,sharing=shared \
    mvn --batch-mode --no-transfer-progress \
    -pl CPPMSEureka -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="CPP Eureka Server" \
      org.opencontainers.image.description="Registro y descubrimiento de servicios CPP"

RUN addgroup -S cpp && adduser -S cpp -G cpp

WORKDIR /app
COPY --from=build /workspace/CPPMSEureka/target/cpp-eureka-server-*.jar app.jar

USER cpp
EXPOSE 8761

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
