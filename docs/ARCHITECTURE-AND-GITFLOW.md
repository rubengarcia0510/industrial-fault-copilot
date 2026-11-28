# Architecture and GitFlow

## 1. Architectural style

Industrial Fault Copilot adopts Hexagonal Architecture
(Ports and Adapters), organized around the diagnostic use case.

This architecture is being introduced incrementally.
It must not be described as fully implemented until its
dependency boundaries have been implemented and verified.

### Dependency rule

Dependencies point inward:

- Inbound adapters translate external requests.
- Application services coordinate use cases through ports.
- Outbound adapters implement those ports.
- Application logic must not depend on infrastructure implementations.

### Intended components

- `DiagnosticController`: inbound HTTP adapter.
- `DiagnosticService`: application use case and orchestration.
- `TechnicalDocumentRepository`: evidence retrieval port.
- `ClasspathTechnicalDocumentRepository`: deterministic evidence adapter.
- `TechnicalKnowledgeService`: application-facing evidence service.
- `DiagnosticProvider`: diagnostic generation port.
- `GeminiDiagnosticAdapter`: Gemini outbound adapter.

### Application responsibilities

`DiagnosticService` must:

- Depend on interfaces, not concrete infrastructure classes.
- Retrieve relevant technical evidence.
- Avoid calling the provider when evidence is empty.
- Validate that returned references belong to retrieved evidence.
- Escalate missing, ambiguous, unsafe, unsupported, or unavailable cases.

The application layer must never call the Gemini SDK directly.

Changing the evidence repository or AI provider must not require
rewriting the diagnostic use case.

### Testing boundaries

- Application tests use mocked ports.
- Tests require no network access or Gemini credentials.
- Adapter tests verify infrastructure-specific behavior.
- HTTP tests verify the external contract.

Do not introduce abstractions without a concrete responsibility.

## 2. Current implementation status

- APIACC-23: HTTP contract and validation integrated through PR #8.
- APIACC-24: deterministic evidence repository pending.
- APIACC-25: Gemini provider pending.
- APIACC-26: orchestration and safe escalation pending.
- APIACC-27: integration and acceptance tests pending.
- APIACC-28: Docker, README, and reproducible demo pending.

The HTTP contract alone does not mean the complete hexagonal
architecture or diagnostic use case is implemented.

Jira under APIACC-22 remains the source of truth for scope,
acceptance criteria, dependencies, and status.

## 3. GitFlow strategy

The repository follows a lightweight GitFlow convention.

| Branch | Purpose | Destination |
| --- | --- | --- |
| `main` | Stable production baseline | Releases and hotfixes |
| `develop` | Integration branch | Features and documentation |
| `feature/*` | Feature implementation | PR into `develop` |
| `docs/*` | Documentation | PR into `develop` |
| `release/*` | Release stabilization | PR into `main` |
| `hotfix/*` | Urgent production fix | PR into `main` |

### Rules

1. Never develop directly on `main` or `develop`.
2. Create feature and documentation branches from latest `develop`.
3. Open feature and documentation PRs against `develop`.
4. Run tests and build checks before integration.
5. Do not merge without explicit repository-owner approval.
6. Create release branches from `develop` when the MVP is ready.
7. Merge accepted releases into `main`.
8. Propagate release fixes back into `develop`.
9. Create hotfix branches from `main`.
10. Propagate integrated hotfixes into `develop`.
11. Do not create release tags before acceptance.

### Naming examples

- `feature/APIACC-24-deterministic-evidence-repository`
- `feature/APIACC-25-gemini-provider`
- `docs/project-architecture-and-gitflow`
- `release/0.1.0`
- `hotfix/diagnostic-validation`

## 4. Change workflow

For every feature:

1. Verify Jira scope, acceptance criteria, and dependencies.
2. Identify exact classes, methods, and tests.
3. Create a branch from latest `develop`.
4. Implement only the agreed scope.
5. Run relevant tests and the Gradle build.
6. Push the branch and open a PR into `develop`.
7. Review the diff and CI results.
8. Merge only after explicit approval.

Never start dependent work before its prerequisites are integrated.
