# Reproducible Docker Demo

This guide demonstrates the Industrial Fault Copilot HTTP API using synthetic technical documents.

## 1. Requirements

- Docker installed and running.
- A Gemini API key with available quota.
- Java 21 and Gradle are not required on the host.

The application does not connect to printers, PLCs, controllers, sensors, or production equipment.

## 2. Build the Docker image

From the repository root:

    docker build -t industrial-fault-copilot:demo .

## 3. Configure Gemini

Set the API key without placing it in repository files:

    export GEMINI_API_KEY="your-api-key"

The default model is `gemini-2.5-flash`.
It can be overridden with `GEMINI_MODEL`.

Start the container:

    docker run -d --name industrial-fault-copilot -p 8080:8080 -e SPRING_AI_MODEL_CHAT=google-genai -e GEMINI_API_KEY="$GEMINI_API_KEY" -e GEMINI_MODEL=gemini-2.5-flash industrial-fault-copilot:demo

Never commit the API key or include it in the Docker image.

## 4. Verify application health

Run:

    curl --fail http://localhost:8080/actuator/health

Expected response:

    {"status":"UP"}

## 5. Scenario A: matching technical evidence

Send the request:

    curl --fail -X POST http://localhost:8080/api/v1/diagnostics -H "Content-Type: application/json" --data @docs/demo-evidence-request.json

Expected behavior:

- The request matches synthetic document `SYN-PRINTER-003`.
- The diagnostic provider receives the matching evidence.
- The API returns a structured JSON diagnosis.
- The response includes hypotheses, safe checks and evidence references.
- Evidence references must correspond to retrieved technical documents.

The generated diagnostic text may vary. Do not expect a fixed Gemini response.

## 6. Scenario B: no matching technical evidence

Send the request:

    curl --fail -X POST http://localhost:8080/api/v1/diagnostics -H "Content-Type: application/json" --data @docs/demo-no-evidence-request.json

Expected behavior:

- No technical document matches `PAPER-99`.
- The diagnostic provider is not invoked.
- The response indicates insufficient technical evidence.
- `evidenceReferences` is empty.
- `escalationRequired` is `true`.

Expected summary:

    "Insufficient technical evidence"

This scenario demonstrates that the application escalates instead of inventing a diagnosis.

## 7. Stop and remove the container

Run:

    docker rm -f industrial-fault-copilot

## 8. Troubleshooting

Inspect application logs:

    docker logs industrial-fault-copilot

Check container status:

    docker ps -a

If the application does not start, verify that the Gemini API key is valid and that the configured model is available.

If Gemini is unavailable or the provider fails, the API must not fabricate a diagnosis. Follow the application's escalation behavior.

## 9. Safety boundaries

- The application provides decision support, not autonomous maintenance.
- It does not connect to printers, PLCs, controllers, sensors, or production equipment.
- It does not execute commands or perform physical actions.
- Only retrieved technical evidence can support diagnostic conclusions.
- Operator observations are treated as unverified input.
- Cases without sufficient evidence must be escalated to a human technician.
- Demo documents are synthetic and do not represent real manufacturer documentation.
