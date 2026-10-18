---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are MANDATORY (Constituição, Princípio III — TDD com cobertura 100%). Every user story MUST include unit test tasks (JUnit 5 + Mockito) and integration test tasks (`@SpringBootTest` + MockMvc) that are written and FAIL before the implementation tasks. The final phase MUST include a `./mvnw verify` task confirming `jacoco:check` at 100%.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Spring Boot / Maven (DEFAULT)**: `src/main/java/com/task/task_backend/{controller,service,repository,dto,entity,mapper,config,exception}/`, `src/main/resources/db/migration/`, `src/test/java/com/task/task_backend/{unit,integration}/`
- **Web app**: `backend/src/`, `frontend/src/`
- **Mobile**: `api/src/`, `ios/src/` or `android/src/`
- Paths shown below assume the Spring Boot layout - adjust based on plan.md structure
- Nomes de arquivos, classes e testes em português (ex.: `TarefaService.java`, `TarefaServiceTest.java`)

<!--
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.

  The /speckit-tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/

  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment

  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Initialize [language] project with [framework] dependencies
- [ ] T003 [P] Configure linting and formatting tools

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

Examples of foundational tasks (adjust based on your project):

- [ ] T004 Setup H2 (modo arquivo) + Flyway baseline migration in src/main/resources/db/migration/V1__*.sql
- [ ] T005 [P] Configure Spring Security OAuth2 Resource Server (Keycloak JWT) in src/main/java/com/task/task_backend/config/SegurancaConfig.java
- [ ] T006 [P] Configure springdoc-openapi (Swagger UI, bearerAuth scheme) in src/main/java/com/task/task_backend/config/OpenApiConfig.java
- [ ] T007 Create base entities/DTOs (records) that all stories depend on
- [ ] T008 Configure @RestControllerAdvice with ProblemDetail and SLF4J logging in src/main/java/com/task/task_backend/exception/
- [ ] T009 Setup application-{perfil}.yml profiles and externalized configuration
- [ ] T010 [P] Configure JaCoCo `check` at 100% (lines + branches) in pom.xml with justified exclusions
- [ ] T011 [P] Create Dockerfile (multi-stage) and compose.yaml (app + Keycloak + volume H2) with keycloak/realm-export.json

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1 (MANDATORY — Princípio III) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T012 [P] [US1] Unit test for [Service] in src/test/java/com/task/task_backend/unit/service/[Nome]ServiceTest.java
- [ ] T013 [P] [US1] Unit test for [Controller] (WebMvcTest + spring-security-test) in src/test/java/com/task/task_backend/unit/controller/[Nome]ControllerTest.java
- [ ] T014 [P] [US1] Integration test for [user journey] in src/test/java/com/task/task_backend/integration/[Nome]IT.java

### Implementation for User Story 1

- [ ] T015 [P] [US1] Create Flyway migration V{n}__[descricao].sql in src/main/resources/db/migration/
- [ ] T016 [P] [US1] Create [Entidade] entity (Lombok, sem @Data) in src/main/java/com/task/task_backend/entity/[Entidade].java
- [ ] T017 [P] [US1] Create DTOs (records, @Schema em português) in src/main/java/com/task/task_backend/dto/
- [ ] T018 [P] [US1] Create [Entidade]Mapper (MapStruct) in src/main/java/com/task/task_backend/mapper/
- [ ] T019 [US1] Create [Entidade]Repository in src/main/java/com/task/task_backend/repository/
- [ ] T020 [US1] Implement [Nome]Service in src/main/java/com/task/task_backend/service/ (depends on T016–T019)
- [ ] T021 [US1] Implement [Nome]Controller with @Operation/@ApiResponses and @PreAuthorize in src/main/java/com/task/task_backend/controller/
- [ ] T022 [US1] Add validation (Bean Validation, mensagens em português) and business exceptions
- [ ] T023 [US1] Add logging (SLF4J via @Slf4j) for user story 1 operations

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2 (MANDATORY — Princípio III) ⚠️

- [ ] T024 [P] [US2] Unit tests for [Service]/[Controller] in src/test/java/com/task/task_backend/unit/
- [ ] T025 [P] [US2] Integration test for [user journey] in src/test/java/com/task/task_backend/integration/[Nome]IT.java

### Implementation for User Story 2

- [ ] T026 [P] [US2] Create migration + [Entidade] entity + DTOs + Mapper
- [ ] T027 [US2] Implement [Nome]Service in src/main/java/com/task/task_backend/service/
- [ ] T028 [US2] Implement [Nome]Controller (OpenAPI + segurança) in src/main/java/com/task/task_backend/controller/
- [ ] T029 [US2] Integrate with User Story 1 components (if needed)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3 (MANDATORY — Princípio III) ⚠️

- [ ] T030 [P] [US3] Unit tests for [Service]/[Controller] in src/test/java/com/task/task_backend/unit/
- [ ] T031 [P] [US3] Integration test for [user journey] in src/test/java/com/task/task_backend/integration/[Nome]IT.java

### Implementation for User Story 3

- [ ] T032 [P] [US3] Create migration + [Entidade] entity + DTOs + Mapper
- [ ] T033 [US3] Implement [Nome]Service in src/main/java/com/task/task_backend/service/
- [ ] T034 [US3] Implement [Nome]Controller (OpenAPI + segurança) in src/main/java/com/task/task_backend/controller/

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX [P] Documentation updates (README em português) in docs/
- [ ] TXXX Code cleanup and refactoring
- [ ] TXXX Performance optimization across all stories
- [ ] TXXX Cover remaining branches to reach 100% JaCoCo (lines + branches)
- [ ] TXXX Verify Swagger UI (/swagger-ui.html) reflects all endpoints with descrições em português
- [ ] TXXX Security hardening (endpoints públicos explicitamente listados, deny-by-default)
- [ ] TXXX Run `./mvnw verify` (tests + jacoco:check 100%) and `docker compose up --build`
- [ ] TXXX Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Within Each User Story

- Tests MUST be written and FAIL before implementation (Princípio III)
- Migration → entity → DTO/mapper → repository → service → controller
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (mandatory):
Task: "Unit test for [Service] in src/test/java/com/task/task_backend/unit/service/[Nome]ServiceTest.java"
Task: "Integration test for [user journey] in src/test/java/com/task/task_backend/integration/[Nome]IT.java"

# Launch all entities/DTOs for User Story 1 together:
Task: "Create [Entidade] entity in src/main/java/com/task/task_backend/entity/[Entidade].java"
Task: "Create DTOs (records) in src/main/java/com/task/task_backend/dto/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
