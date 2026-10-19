# syntax=docker/dockerfile:1
# Imagen de inventario-service (multi-stage). Construir desde la carpeta backend/:
#   docker build -f inventario-service/Dockerfile -t fiadoya/inventario-service .
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -pl inventario-service -am package -DskipTests -Djacoco.skip=true \
    && cp inventario-service/target/inventario-service-1.0.0.jar /app.jar

FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --home-dir /app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /app.jar app.jar
USER app
EXPOSE 8082
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC" \
    TZ=UTC
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8082 && printf "GET /actuator/health/readiness HTTP/1.0\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
