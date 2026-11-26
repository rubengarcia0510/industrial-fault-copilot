# Agent Stack 48 — Implementation Plan

## 1. Goal

Build **Industrial Fault Copilot**, an independent specialized AI agent that helps an operator understand and perform an initial diagnosis of faults in digital printing equipment.

The operator submits:
- Equipment make/model or a descriptive equipment identifier.
- The observed symptom.
- An optional alarm/error code.
- Optional operator observations.

The agent searches a small, curated technical knowledge base and returns:
- A concise explanation of what the available evidence suggests.
- Possible causes, clearly identified as hypotheses.
- Safe, non-invasive checks an operator can perform.
- References to the exact documents/sections used.
- An explicit uncertainty level and whether a technician must intervene.

The project is independently developed and uses only public, openly licensed, or independently authored synthetic demonstration material.

## 2. Explicit MVP boundaries

Included:
- Digital printing equipment as the target domain.
- HTTP diagnostic API.
- Gemini-backed diagnostic generation.
- Retrieval from a small set of public, openly licensed, or independently authored synthetic demo documents.
- Traceable evidence in every supported diagnosis.
- Human escalation when evidence is missing, ambiguous, or potentially unsafe.
- Automated tests and a reproducible Docker demo.

Excluded:
- Any connection to printers, PLCs, controllers, sensors, vendor networks, or machine APIs.
- Machine control, parameter changes, or automated maintenance actions.
- Confidential manuals, photographs, diagrams, logs, machine identifiers, or operational data.
- Claims that synthetic demo documents are genuine manufacturer manuals.
- Vector database, complex RAG infrastructure, multi-agent orchestration, frontend application, and production deployment.
- MCP server in the MVP. It is optional stretch work only after all MVP acceptance criteria pass.

## 3. Stack and constraints

- Java 21.
- Existing Spring Boot 4.0.8 / Spring WebFlux baseline.
- Gradle Kotlin DSL.
- Spring AI 2.0.x, subject to dependency compatibility verification during the build window.
- Google Gemini API, using a currently available Flash model selected through configuration.
- Docker.

Budget target: USD 0. Do not enable billing or rely on paid services. API key and model name must come from environment variables. If free-tier quota is unavailable, the demo must report the provider as unavailable; it must not pretend a fallback result came from Gemini.

## 4. Existing API baseline and exact implementation targets

PR #4 currently introduces these files and endpoint:

- `src/main/java/com/rubengarcia/industrialfaultcopilot/api/DiagnosticController.java`
  - `POST /api/v1/diagnostics`
  - Add `@Valid` to the request body and preserve the endpoint contract.
- `src/main/java/com/rubengarcia/industrialfaultcopilot/diagnostic/DiagnosticService.java`
  - Replace the placeholder response in `diagnose(DiagnosticRequest request)` with orchestration.
  - It must not call the Gemini SDK or HTTP client directly.
- `src/main/java/com/rubengarcia/industrialfaultcopilot/diagnostic/dto/DiagnosticRequest.java`
  - Keep the existing package and record.
  - Required non-blank fields: `machine` and `symptom`.
  - Optional fields: `alarmCode` and `observations`.
  - `observations` is free-text context supplied by the operator and must not be treated as verified technical evidence.
- `src/main/java/com/rubengarcia/industrialfaultcopilot/diagnostic/dto/DiagnosticResponse.java`
  - Preserve the existing response fields unless tests demonstrate a necessary, reviewed change.

Add the following application and adapter types:

- `src/main/java/com/rubengarcia/industrialfaultcopilot/diagnostic/DiagnosticProvider.java`
  - Application port.
  - Method: `Mono<DiagnosticResponse> diagnose(DiagnosticRequest request, List<TechnicalEvidence> evidence)`.
- `src/main/java/com/rubengarcia/industrialfaultcopilot/knowledge/TechnicalEvidence.java`
  - Evidence record containing documentId, title, excerpt, and sourceLocation.
- `src/main/java/com/rubengarcia/industrialfaultcopilot/knowledge/TechnicalDocumentRepository.java`
  - Retrieval port.
  - Method: `Mono<List<TechnicalEvidence>> search(DiagnosticRequest request)`.
- `src/main/java/com/rubengarcia/industrialfaultcopilot/knowledge/TechnicalKnowledgeService.java`
  - Delegates to the repository and returns only matching evidence.
- `src/main/java/com/rubengarcia/industrialfaultcopilot/knowledge/ClasspathTechnicalDocumentRepository.java`
  - Small deterministic implementation over curated classpath demo documents. No vector database.
- `src/main/java/com/rubengarcia/industrialfaultcopilot/ai/GeminiDiagnosticAdapter.java`
  - Implements DiagnosticProvider using Spring AI.
  - Receives the request and retrieved evidence.
  - Requests a constrained structured response.
  - Must not add facts, alarm meanings, sources, or procedures absent from evidence.
  - Applies a bounded timeout and controlled error handling.

Keep the implementation small. Do not introduce extra layers or abstractions unless required by the existing code or tests.

## 5. Knowledge and source policy

Store demo documents under `src/main/resources/knowledge/`.

Every document must state its provenance and whether it is public/openly licensed or synthetic. Use only material that can be independently included and redistributed.

For the hackathon demo, synthetic documents are acceptable only when visibly labelled as synthetic examples. Their content must not be represented as verified manufacturer instructions or safe repair procedures.

Retrieval starts with deterministic matching against equipment identifier, symptom keywords, and optional alarm code. Every evidence result must preserve a stable document ID, title, and section/location.

If no relevant evidence is found:
- Do not ask Gemini to invent a diagnosis.
- Return an explicit insufficient-evidence result.
- Set escalationRequired to true.

## 6. Diagnostic behavior and safety

The agent may explain evidence and suggest only low-risk, non-invasive observations (for example, checking whether an alarm is still displayed or recording the exact message). It must not instruct an unqualified operator to open electrical cabinets, bypass interlocks, touch energized components, alter machine parameters, or perform repairs.

Rules:
- Distinguish documented facts from hypotheses.
- Never invent alarm-code meanings, machine specifications, sources, or procedures.
- Treat operator observations as unverified input, not as authoritative documentation.
- Every technical claim must be supported by returned evidence or explicitly labelled as a general, unverified possibility.
- Unknown or conflicting information must be stated plainly.
- Escalate electrical, mechanical, thermal, pressure-related, safety-interlock, or otherwise hazardous cases to a qualified technician.
- Never control a machine or trigger an external action.

## 7. Test plan

Tests must not require a real Gemini API key or network access.

### API and request validation
- Valid request reaches the service.
- Blank/missing machine is rejected.
- Blank/missing symptom is rejected.
- Optional alarmCode is accepted.
- Optional observations are accepted and propagated to the application service.
- Observations are not mistaken for retrieved technical evidence.
- Existing response JSON contract is preserved.

### Application orchestration
- Matching evidence is passed to the provider.
- Operator observations are passed as contextual input.
- Empty evidence returns insufficient evidence and escalation without invoking Gemini.
- Provider success is returned with source references.
- Provider error/timeout produces a controlled failure and escalation.
- Unsafe or uncertain result requires escalation.

### Knowledge retrieval
- Equipment and symptom match.
- Exact alarm-code match.
- No matching document.
- Multiple matching documents.
- Source ID and section are preserved.
- Synthetic provenance is visible.

### Gemini adapter
- Structured valid response maps correctly.
- Invalid/malformed output is rejected.
- Evidence is included in model input.
- Operator observations are clearly distinguished from technical evidence in model input.
- Unsupported claims or unknown source IDs are rejected.
- Timeout and provider error are handled.
- Missing API key does not expose secrets or fabricate a response.

Use mocked provider/repository implementations for deterministic unit tests. No test may call Gemini.

## 8. Acceptance criteria

The MVP is complete when:

1. `POST /api/v1/diagnostics` validates its input.
2. The request accepts machine, symptom, optional alarmCode, and optional observations.
3. The service retrieves relevant technical evidence.
4. No-evidence cases stop before the model call and require escalation.
5. Gemini returns a structured diagnostic grounded in retrieved evidence.
6. Every returned source maps to a real document ID and location in the demo knowledge base.
7. Unsafe, ambiguous, unsupported, and provider-failure cases are handled conservatively.
8. Automated tests pass without external credentials.
9. `./gradlew build` passes.
10. Docker image builds and the documented demo request can be reproduced.
11. Repository and presentation clearly identify this as an independent hackathon implementation using public or synthetic demo material.

## 9. Build-window plan (Argentina time)

Official window: Friday 11 December 2026, 13:00 to Sunday 13 December 2026, 13:00.

Available work time: Friday 13:00–24:00 (11 hours), Saturday unavailable (0 hours), Sunday 00:00–13:00 (13 hours). Total: 24 effective hours.

### Friday 11 December
- 13:00–15:00 — Verify baseline, complete API validation and tests.
- 15:00–18:00 — Implement deterministic knowledge retrieval and safe demo documents.
- 18:00–22:00 — Implement Gemini adapter, configuration, and mocked tests.
- 22:00–24:00 — Integrate API, evidence, provider, and escalation flow.

### Saturday 12 December
- No work scheduled.

### Sunday 13 December
- 00:00–03:00 — End-to-end tests and failure-path hardening.
- 03:00–06:00 — Docker build, reproducible demo, and README.
- 06:00–08:00 — Verify all acceptance criteria and fix critical defects.
- 08:00–10:00 — Record demo and prepare presentation/cover.
- 10:00–12:00 — Final repository and submission checks.
- 12:00–13:00 — Submission buffer.

Do not add scope to compensate for Saturday unavailability. If time is short, prioritize a reliable evidence-grounded agent over MCP, UI, extra integrations, or deployment.

## 10. Post-MVP stretch goal

Only if all acceptance criteria pass and there is remaining time, consider exposing the same application use case through one MCP tool. This must reuse the application service, preserve evidence and escalation metadata, and must not create a machine-control capability. It is not required for submission.

## 11. Submission proof

Prepare:
- Public GitHub repository with setup and architecture documentation.
- Reproducible Docker-based demo.
- Recorded walkthrough showing one evidence-supported diagnosis and one no-evidence/escalation case.
- Project description that states the independent scope and the lack of direct machine connectivity.
- Cover image and concise presentation.

Public materials must describe only this independent implementation, its own functionality, and its public or synthetic demo sources.
