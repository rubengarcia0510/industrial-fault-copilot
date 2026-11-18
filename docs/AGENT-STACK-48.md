# Agent Stack 48 — Implementation Plan

## Objective

Evolve Industrial Fault Copilot into a specialized
industrial diagnostic agent with a reusable MCP server.

## Stack

- Java 21
- Spring Boot 4.0.8
- Spring AI 2.0.x
- Google Gemini API
- MCP Server with WebFlux
- Streamable HTTP transport
- Gradle Kotlin DSL
- Docker

## Existing implementation

- Spring Boot application baseline
- WebFlux and Actuator
- Health endpoints and tests
- Docker configuration
- Evolus client configuration
- PR #4 contains the initial diagnostic API contract

PR #4 must be reviewed and completed before extending
the diagnostic API. Its current diagnostic service
is only a placeholder.

## Implementation stages

### 1. Diagnostic API

Review and complete:

- DiagnosticController
- DiagnosticService
- DiagnosticRequest
- DiagnosticResponse

### 2. Gemini integration

Create:

- GeminiConfiguration
- GeminiDiagnosticAdapter

Use Spring AI and structured diagnostic responses.

### 3. Industrial knowledge

Create:

- TechnicalKnowledgeService
- TechnicalDocumentRepository

Provide traceable technical evidence for diagnoses.

### 4. MCP server

Create:

- McpServerConfiguration
- IndustrialDiagnosticTools

Expose:

- searchTechnicalManual
- getAlarmCodeDetails
- getSafetyProcedure
- analyzeEquipmentFault

Use WebFlux and Streamable HTTP.

### 5. Integration and demonstration

- Diagnostic API integration tests
- Gemini adapter tests
- Knowledge retrieval tests
- MCP tool discovery and execution tests
- Docker build
- Reproducible end-to-end demonstration

## Safety requirements

- Never invent technical evidence.
- Unknown alarm codes must be identified as unknown.
- Recommendations must include their supporting sources.
- Uncertain or potentially dangerous cases require
  human intervention.
- No direct machinery control in the MVP.

## Acceptance criteria

A user can submit a machine, symptom and alarm code,
receive an evidence-based diagnostic response, and
allow an external MCP-compatible agent to access the
same diagnostic capabilities.

