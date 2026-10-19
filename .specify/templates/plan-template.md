# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or NEEDS CLARIFICATION]

**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or NEEDS CLARIFICATION]

**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]

**Testing**: [e.g., pytest, XCTest, cargo test or NEEDS CLARIFICATION]

**Target Platform**: [e.g., Linux server, iOS 15+, WASM or NEEDS CLARIFICATION]

**Project Type**: [e.g., library/cli/web-service/mobile-app/compiler/desktop-app or NEEDS CLARIFICATION]

**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]

**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]

**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Avaliar cada portão abaixo contra `.specify/memory/constitution.md` (v1.0.0). Marcar
✅ (conforme), ⚠ (violação justificada em Complexity Tracking) ou ❌ (bloqueio).

| # | Princípio | Portão | Status |
|---|-----------|--------|--------|
| I | Arquitetura em Camadas | Novas classes mapeadas para `controller`/`service`/`repository`/`dto`/`entity`/`mapper`/`config`/`exception`; entidades não expostas em endpoints; serviços sem estado | [ ] |
| II | Idioma Português | Nomes, mensagens, Javadoc, Swagger e documentos desta feature em português | [ ] |
| III | Testes Primeiro / 100% | Tarefas de teste (unidade + integração) precedem implementação; `jacoco:check` a 100% mantido; exclusões justificadas | [ ] |
| IV | Segurança Keycloak | Endpoints protegidos por JWT; roles mapeadas; nenhum armazenamento local de credenciais | [ ] |
| V | Contrato OpenAPI | `@Operation`/`@ApiResponses`/`@Schema` em português; contrato salvo em `contracts/` | [ ] |
| VI | Persistência H2 + Flyway | Alterações de esquema via `V{n}__*.sql`; `ddl-auto=validate`; H2 em arquivo fora dos testes | [ ] |
| VII | Docker | `Dockerfile`/`compose.yaml` atualizados se dependências/perfis mudaram; sem segredos em código | [ ] |
| VIII | Simplicidade / Lombok | DTOs como `record`; injeção por construtor; abstrações extras justificadas | [ ] |

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: Spring Boot / Maven (DEFAULT — Princípio I da constituição)
src/main/java/com/task/task_backend/
├── controller/
├── service/
├── repository/
├── dto/
├── entity/
├── mapper/
├── config/
└── exception/
src/main/resources/
├── application.yml
├── application-{perfil}.yml
└── db/migration/            # Flyway V{n}__descricao.sql

src/test/java/com/task/task_backend/
├── unit/                    # JUnit 5 + Mockito, por camada
└── integration/             # @SpringBootTest + MockMvc, H2 + Keycloak efêmero
src/test/resources/

Dockerfile
compose.yaml
keycloak/realm-export.json

# [REMOVE IF UNUSED] Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure: feature modules, UI flows, platform tests]
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
