# Agent Stack 48 — Implementation Plan

## 1. Objective

Evolve Industrial Fault Copilot into a specialized industrial
diagnostic agent with a reusable MCP server.

The MVP receives equipment information, symptoms and alarm codes,
returns evidence-based diagnostic results and exposes the same
capabilities through MCP.

No direct machinery control is included.

## 2. Technology stack

- Java 21
- Spring Boot 4.0.8
- Spring AI 2.0.x
- Google Gemini API
- Spring WebFlux
- MCP Server with Streamable HTTP
- Gradle Kotlin DSL
- Docker

Spring AI 2.0.x is selected to remain compatible with the
existing Spring Boot 4.0.8 baseline.

## 3. Architecture and package structure

Use the existing base package:
`com.rubengarcia.industrialfaultcopilot`

### diagnostic

`api/DiagnosticController.java`
- Expose POST `/api/v1/diagnostics`.
- Validate the request.
- Delegate to `DiagnosticService`.
- Return the diagnostic response.

`diagnostic/DiagnosticService.java`
- Orchestrate the diagnostic use case.
- Retrieve relevant technical evidence.
- Invoke the diagnostic provider.
- Apply escalation rules.
- Never access Gemini SDK or WebClient directly.

`diagnostic/DiagnosticRequest.java`
- Fields: machine, symptom and alarmCode.
- Validate required fields.

`diagnostic/DiagnosticResponse.java`
- Fields: diagnosis, recommendations, sources and
  escalationRequired.
- Preserve the existing API contract unless a reviewed
  compatibility change is required.

### ai

`ai/DiagnosticProvider.java`
- Application port for diagnostic generation.
- Method:
  `Mono<DiagnosticResponse> diagnose(DiagnosticRequest request, List<TechnicalEvidence> evidence)`

`ai/GeminiDiagnosticAdapter.java`
- Implement DiagnosticProvider using Spring AI.
- Convert the request and evidence into a constrained prompt.
- Request structured output.
- Reject malformed or unsupported responses.
- Apply provider timeout and error handling.

`ai/GeminiConfiguration.java`
- Configure the Google GenAI model.
- Read credentials and model selection from environment-backed
  Spring properties.
- Never store credentials in source control.

### knowledge

`knowledge/TechnicalEvidence.java`
- Represent a technical source with document ID, title,
  relevant content and reference location.

`knowledge/TechnicalKnowledgeService.java`
- Method:
  `Mono<List<TechnicalEvidence>> search(DiagnosticRequest request)`
- Retrieve evidence relevant to equipment, symptom and alarm code.
- Return an empty list when no evidence is available.

`knowledge/TechnicalDocumentRepository.java`
- Define the retrieval port for technical documents.
- Method:
  `Mono<List<TechnicalEvidence>> search(String machine, String symptom, String alarmCode)`

MVP retrieval strategy:
- Store versioned Markdown technical documents under
  `src/main/resources/knowledge/`.
- Include metadata for equipment, manufacturer, alarm codes,
  document title and source reference.
- Start with deterministic text and metadata matching.
- Do not introduce a vector database in the MVP.
- Keep the repository interface independent from the storage
  implementation so retrieval can evolve later.

### mcp

`mcp/McpServerConfiguration.java`
- Configure the Spring AI MCP Server.
- Use WebFlux and Streamable HTTP.
- Register the diagnostic tools.
- Keep the transport behind an explicit security boundary.

`mcp/IndustrialDiagnosticTools.java`
Expose these tools:

- `searchTechnicalManual(machine, symptom, alarmCode)`
- `getAlarmCodeDetails(machine, alarmCode)`
- `getSafetyProcedure(machine, procedureType)`
- `analyzeEquipmentFault(machine, symptom, alarmCode)`

All tools must delegate to application services.
They must not access repositories or Gemini directly.

## 4. Gemini configuration

Use Spring AI Google GenAI starter compatible with Spring AI 2.0.x.

Configuration must be externalized through environment variables:

- `GOOGLE_GENAI_API_KEY`
- `GOOGLE_GENAI_MODEL`

Map these variables through Spring configuration.
Do not commit API keys or local secret files.

Expected behavior:
- Missing API key: application startup or readiness must report
  the integration as unavailable, without exposing secrets.
- Provider timeout: return a controlled diagnostic failure.
- Provider error: log safe technical context and require escalation.
- Invalid structured output: reject it; never fabricate a diagnosis.

## 5. Diagnostic flow

1. Receive and validate the diagnostic request.
2. Search the technical knowledge repository.
3. If no relevant evidence exists, mark the result as uncertain
   and require human escalation.
4. Invoke DiagnosticProvider with the request and evidence.
5. Validate the structured response.
6. Ensure recommendations reference supporting evidence.
7. Apply escalation rules.
8. Return the API response.

Unknown alarm codes must remain explicitly unknown.
The model must not invent alarm meanings, manual references
or safety procedures.

## 6. Implementation stages and dependencies

### Stage 1 — Complete diagnostic API

Depends on reviewing and merging PR #4.

Files:
- DiagnosticController
- DiagnosticService
- DiagnosticRequest
- DiagnosticResponse
- DiagnosticProvider

Acceptance:
- Existing API contract remains compatible.
- Request validation is covered by tests.
- Service orchestration is independent of provider implementation.
- No Gemini-specific code exists in the diagnostic service.

Tests:
- Valid request.
- Invalid request.
- Provider success.
- Provider failure.
- Missing technical evidence.
- Escalation required.

### Stage 2 — Gemini adapter

Depends on Stage 1.

Files:
- GeminiConfiguration
- GeminiDiagnosticAdapter

Acceptance:
- Gemini receives the request and evidence.
- Output is mapped to the diagnostic response.
- Invalid output is rejected.
- Missing credentials are handled safely.
- Timeouts and provider errors are controlled.

Tests:
- Valid structured response.
- Invalid structured response.
- Provider timeout.
- Provider error.
- Missing API key.
- Evidence included in model input.

### Stage 3 — Technical knowledge

Depends on the evidence contract from Stage 1.

Files:
- TechnicalEvidence
- TechnicalKnowledgeService
- TechnicalDocumentRepository
- Markdown document resources

Acceptance:
- Documents can be retrieved by equipment and alarm code.
- Each result includes traceable source metadata.
- Unknown codes return no fabricated information.
- Empty retrieval results trigger escalation.

Tests:
- Exact alarm code match.
- Equipment match.
- No matching document.
- Missing metadata.
- Multiple matching documents.
- Source reference preservation.

### Stage 4 — MCP server

Depends on Stages 1 and 3.

Files:
- McpServerConfiguration
- IndustrialDiagnosticTools

Acceptance:
- MCP clients can discover the four tools.
- Tools invoke application services.
- Tool results preserve evidence and escalation information.
- No tool can issue machine-control commands.
- HTTP transport is not exposed publicly without authentication
  or an explicitly documented trusted network boundary.

Tests:
- Tool discovery.
- Each tool execution.
- Invalid tool arguments.
- Empty evidence.
- Diagnostic escalation.
- Transport security configuration.

### Stage 5 — Integration and demonstration

Depends on Stages 1–4.

Acceptance:
- Complete diagnostic flow works from HTTP API.
- The same diagnostic capabilities work through MCP.
- Results include traceable evidence.
- Uncertain cases require human intervention.
- Docker image builds successfully.
- Demo can be reproduced from documented instructions.

Validation commands:
- `./gradlew test`
- `./gradlew build`
- `docker build -t industrial-fault-copilot .`

## 7. Safety requirements

- Never invent technical evidence.
- Unknown alarm codes must be identified as unknown.
- Recommendations must include supporting sources.
- Uncertain or potentially dangerous cases require human review.
- No direct machinery control in the MVP.
- Do not expose credentials, prompts containing secrets,
  or sensitive operational data in logs.
- MCP transport must have an explicit security boundary.

## 8. Scope boundaries

Included:
- Industrial diagnostic API.
- Gemini-backed diagnostic provider.
- Search over versioned technical documents.
- MCP tools exposing the same application capabilities.
- Automated tests and reproducible Docker demonstration.

Excluded from MVP:
- Direct PLC or machine control.
- Automatic equipment parameter changes.
- Vector database and semantic indexing.
- Unverified manufacturer integrations.
- Production deployment infrastructure.

## 9. Definition of done

The project is ready for the hackathon demonstration when:

1. A user can submit machine, symptom and alarm code.
2. The system retrieves relevant technical evidence.
3. Gemini produces a validated structured diagnostic response.
4. Recommendations include traceable sources.
5. Unknown or uncertain cases require human escalation.
6. An MCP-compatible client can discover and invoke the tools.
7. Automated tests and Docker build pass.
8. Setup and demo instructions are documented.
