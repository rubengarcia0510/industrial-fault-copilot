# Industrial Fault Copilot

Independent AI-assisted backend for evidence-based initial diagnosis of faults in digital printing equipment.

Developed for the LabLab Agent Stack 48 hackathon using independently authored synthetic demonstration material and technical sources with verifiable redistribution rights.

## Implemented capabilities

- HTTP diagnostic API with request validation.
- Deterministic retrieval of technical evidence.
- Google Gemini integration with structured diagnostic responses.
- Evidence-grounded diagnostic orchestration.
- Conservative handling and human escalation when evidence is insufficient.
- Integration tests covering successful and unavailable-provider scenarios.
- Reproducible Docker-based deployment and demonstration.

## Technology stack

- Java 21
- Spring Boot 4.0.8
- Spring WebFlux
- Gradle Kotlin DSL
- Spring AI with Google Gemini
- Docker

## Prerequisites

- Java 21 for local execution and builds.
- Docker for container builds and execution.
- A Gemini API key with available quota for live diagnostic requests.

The Gradle Wrapper is included. A separate Gradle installation is not required.

## Configuration

The application reads configuration from environment variables:

| Variable | Description | Default |
|---|---|---|
| `SPRING_AI_MODEL_CHAT` | Chat provider | `none` |
| `GEMINI_API_KEY` | Gemini API key | Empty |
| `GEMINI_MODEL` | Gemini model | `gemini-2.5-flash` |

Keep API keys outside source files and never commit secrets.

The project targets a USD 0 budget. Gemini availability depends on the account's credentials and quota; no paid service is required for automated tests.

## Run locally

Start the application:

    ./gradlew bootRun

Check application health:

    curl --fail http://localhost:8080/actuator/health

Run the diagnostic API with Gemini configured:

    SPRING_AI_MODEL_CHAT=google-genai GEMINI_API_KEY="your-api-key" ./gradlew bootRun

## Test

Run the automated test suite:

    ./gradlew test

Tests use mocks and do not require a Gemini API key or network access to the model provider.

## Build

Build the application:

    ./gradlew build

## Docker

Build the image:

    docker build -t industrial-fault-copilot:demo .

Run the container with Gemini configured:

    docker run -d --name industrial-fault-copilot -p 8080:8080 -e SPRING_AI_MODEL_CHAT=google-genai -e GEMINI_API_KEY="$GEMINI_API_KEY" -e GEMINI_MODEL=gemini-2.5-flash industrial-fault-copilot:demo

Check container health:

    curl --fail http://localhost:8080/actuator/health

Stop and remove the container:

    docker rm -f industrial-fault-copilot

See [the reproducible Docker demo](docs/DEMO.md) for complete instructions and both diagnostic scenarios.

## Demo scenarios

The reproducible demo covers two scenarios:

- **Evidence available:** `PAPER-02` retrieves synthetic document `SYN-PRINTER-003` and sends the matching evidence to Gemini.
- **Evidence unavailable:** `PAPER-99` does not invoke the diagnostic provider and returns an escalation response with no evidence references.

Both requests are available under `docs/`.

## Project documentation

- [Project scope, architecture and acceptance criteria](docs/AGENT-STACK-48.md)
- [Reproducible Docker demo](docs/DEMO.md)

## Budget and independence

The target budget is USD 0. Live Gemini requests depend on available credentials and quota. Automated tests do not require paid services.

This is an independent implementation. It does not contain or rely on confidential customer systems, documents, operational data or proprietary prototypes.
