# Industrial Fault Copilot

Evidence-based AI assistant for diagnosing industrial machinery faults. Built for the AMD Developer Hackathon: ACT III.

## Stack
- Java 21
- Spring WebFlux (reactive, Netty)
- Spring Boot 4.0.8
- Gradle 9.x (Kotlin DSL, 9.4 or newer required for Java 21)
- Docker

## Current status
Initial backend baseline only: health endpoints, tests and Docker. Evolus and AMD model integration will be implemented in later stages.

## First-time setup: Gradle Wrapper
This repo does not ship the wrapper JAR. With Gradle 9.4+ installed, run once:

```bash
gradle wrapper --gradle-version 9.4.0
```

Then commit `gradlew`, `gradlew.bat` and `gradle/wrapper/`.

## Run locally
```bash
./gradlew bootRun
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
```

## Test
```bash
./gradlew test
```

## Build
```bash
./gradlew build
```

## Docker
```bash
docker build -t industrial-fault-copilot .
docker run --rm -p 8080:8080 industrial-fault-copilot
curl http://localhost:8080/health
```

## Configuration
Reserved for future stages (none are required to start the app):

| Variable | Purpose |
|---|---|
| `EVOLUS_API_KEY` | Evolus API key |
| `EVOLUS_APPLICATION_ID` | Evolus application id |
| `EVOLUS_AGENT_ID` | Evolus agent id |
| `EVOLUS_BASE_URL` | Evolus API base URL (default `https://api.evolus.ai/api/v1`) |
| `AMD_MODEL_ENDPOINT` | AMD-hosted model endpoint |

Secrets must be supplied through environment variables and must never be committed.
