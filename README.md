# Industrial Fault Copilot

Independent specialized AI agent for evidence-based initial diagnosis of faults in digital printing equipment.

This project is being developed for the LabLab Agent Stack 48 hackathon. It uses independently authored synthetic demonstration material and, where applicable, public technical documents with verifiable redistribution rights.

## Project status

The repository currently contains the initial Java/Spring Boot backend baseline. The diagnostic API, evidence retrieval, Gemini integration, and diagnostic orchestration are planned MVP work and must not be considered implemented yet.

See [the implementation plan](docs/AGENT-STACK-48.md) for the approved scope, architecture, safety constraints, implementation targets, and acceptance criteria.

## Planned MVP capabilities

The planned MVP will allow an operator to submit:
- Equipment identifier or make/model.
- Observed symptom.
- Optional alarm or error code.
- Optional operator observations.

The agent will retrieve relevant technical evidence and, when sufficient evidence is available, use Google Gemini to produce a structured diagnostic response containing:
- Possible causes, explicitly identified as hypotheses.
- Safe, non-invasive checks.
- References to supporting documents and sections.
- Uncertainty and whether escalation to a qualified technician is required.

The agent must not invent technical evidence or present synthetic material as genuine manufacturer documentation.

## Safety boundaries

- No connections to printers, PLCs, controllers, sensors, vendor networks, or machine APIs.
- No machine control, parameter changes, or automated maintenance actions.
- No instructions to bypass safety interlocks, open energized equipment, or perform hazardous repairs.
- Missing, ambiguous, conflicting, or potentially unsafe evidence must result in conservative handling and human escalation.

## Technology stack

- Java 21
- Spring Boot 4.0.8
- Spring WebFlux
- Gradle Kotlin DSL
- Spring AI (planned, subject to compatibility verification)
- Google Gemini API (planned)
- Docker

## Prerequisites

- Java 21
- Docker, only if you want to build and run the container image.

The repository includes the Gradle Wrapper, so a separate Gradle installation is not required.

## Run locally

Start the application:

```bash
./gradlew bootRun
```

Check the health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

The diagnostic API is not implemented yet.

## Test

Run the test suite:

```bash
./gradlew test
```

## Build

Build the application:

```bash
./gradlew build
```

## Docker

Build the image:

```bash
docker build -t industrial-fault-copilot .
```

Run the container:

```bash
docker run --rm -p 8080:8080 industrial-fault-copilot
```

Check container health:

```bash
curl http://localhost:8080/actuator/health
```

## Configuration

The planned Gemini integration will obtain its API key and model configuration from environment variables. Secrets must never be committed.

The target budget is USD 0. The application must handle unavailable credentials or free-tier quota without fabricating a model response.

## Demo and submission

The final submission will include:
- A reproducible Docker-based demo.
- An evidence-supported diagnostic example.
- A no-evidence case that escalates instead of inventing a diagnosis.
- A recorded walkthrough and project presentation.

This repository describes an independent implementation. It does not contain or rely on confidential customer systems, documents, operational data, or proprietary prototypes.
