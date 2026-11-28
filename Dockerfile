FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY . .

RUN chmod +x gradlew \
    && ./gradlew bootJar --no-daemon \
    && JAR_PATH="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" \
    && test -n "$JAR_PATH" \
    && cp "$JAR_PATH" /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --uid 10001 appuser

COPY --from=build /workspace/app.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
