# ETAPA 1: Compilación con Maven (imagen pesada, solo existe durante el build)
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Primero solo el pom.xml: Docker cachea esta capa y NO vuelve a descargar dependencias
# mientras el pom no cambie (los cambios en src/ reutilizan la caché).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Después el código fuente y el empaquetado del .jar ejecutable de Spring Boot.
# -DskipTests: las pruebas se corren fuera del build de la imagen (./mvnw test).
COPY src/ ./src/
RUN mvn -B -q package -DskipTests && \
    cp target/retailservice-*.jar /build/app.jar

# ETAPA 2: Imagen liviana de ejecución (solo JRE, sin Maven ni código fuente)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/app.jar /app/app.jar

# Carpeta donde k8s/03-pvc-and-deployment.yaml monta el volumen persistente (H2 en archivo)
RUN mkdir -p /app/data

EXPOSE 8080

# La JVM usa como máximo el 75% de la memoria que Kubernetes le asigne (resources.limits.memory)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENV APP_ENVIRONMENT=desarrollo

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
