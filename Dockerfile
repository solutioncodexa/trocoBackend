# =============================================================================
# Troco Backend — Dockerfile multi-stage
# =============================================================================
# Stage 1 : build avec Maven + JDK 21
# Stage 2 : runtime JRE 21 minimal + agent OpenTelemetry (auto-instrumentation)
# =============================================================================

# ---------- 1. BUILD ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Layer cache : on copie d'abord le pom et on pre-DL les deps
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

# Puis le code
COPY src ./src

# Build du jar (skip tests : exécutés dans la pipeline CI)
RUN mvn -B -ntp -DskipTests package \
    && cp target/*.jar /workspace/app.jar


# ---------- 2. AGENT OTEL ----------
# Téléchargement de l'agent OpenTelemetry Java pour auto-instrumentation
# (HTTP, JDBC, JPA, Hikari, etc. — toutes les traces sortent en OTLP).
FROM curlimages/curl:8.10.1 AS otel
ARG OTEL_AGENT_VERSION=2.10.0
WORKDIR /tmp
RUN curl -fsSL -o opentelemetry-javaagent.jar \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar"


# ---------- 3. RUNTIME ----------
FROM eclipse-temurin:21-jre-alpine

# tini = init léger, propre gestion des signaux + reaping
# tzdata pour la timezone correcte
RUN apk add --no-cache tini tzdata curl \
    && addgroup -S troco \
    && adduser -S -G troco troco
ENV TZ=Africa/Casablanca

WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
COPY --from=otel /tmp/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

# Volumes : stockage local fallback + logs persistants
RUN mkdir -p /app/uploads /app/logs && chown -R troco:troco /app
VOLUME ["/app/uploads", "/app/logs"]

USER troco

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    OTEL_RESOURCE_ATTRIBUTES="service.name=troco-backend,service.namespace=troco" \
    OTEL_TRACES_EXPORTER=otlp \
    OTEL_METRICS_EXPORTER=none \
    OTEL_LOGS_EXPORTER=none \
    OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf \
    LOG_DIR=/app/logs

EXPOSE 8080

# Healthcheck : actuator health
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["/sbin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS -javaagent:/app/opentelemetry-javaagent.jar -jar /app/app.jar"]
