# Implementation Plan: API de Gerenciamento de Tarefas

**Branch**: `001-gerenciamento-tarefas-api` | **Date**: 2026-09-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-gerenciamento-tarefas-api/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Backend REST de uma lista de tarefas pessoais: cada usuário autenticado no Keycloak
cadastra tarefas com descrição, prioridade, prazo previsto e valor em reais; edita,
conclui/reabre, reordena (topo/final/posição), consulta histórico automático de
alterações, exclui, filtra por situação, busca sem acentos e acompanha o total a gastar
no mês. Implementação em Spring Boot 4.1.1 / Java 21 com arquitetura em camadas
(controller → service → repository, DTOs `record`, MapStruct, Lombok), H2 persistente
em arquivo com Flyway, OAuth2 Resource Server validando JWT do Keycloak, springdoc
OpenAPI em português, cobertura JaCoCo 100% (unidade + integração com Keycloak via
Testcontainers) e entrega via Dockerfile multi-stage + Docker Compose (app + Keycloak).
Interface visual fica para feature futura; este plano garante que o contrato exponha
tudo que ela precisará (indicador `atrasada`, frases prontas de histórico, valores
formatados).

## Technical Context

**Language/Version**: Java 21 (LTS) — `<java.version>21</java.version>`; recursos: `record`, pattern matching, `switch` com padrões

**Primary Dependencies**: Spring Boot 4.1.1 (`starter-webmvc`, `starter-validation`, `starter-data-jpa`, `starter-flyway`, `starter-oauth2-resource-server`, `starter-actuator`), springdoc-openapi-starter-webmvc-ui 3.1.1, Lombok (BOM), MapStruct 1.6.3 + lombok-mapstruct-binding 0.2.0

**Storage**: H2 2.x em modo arquivo (`jdbc:h2:file:./dados/tarefas`; em Docker `/app/dados/tarefas` com volume nomeado), esquema via Flyway `V1__criar_tabelas_tarefa_e_historico.sql`, `ddl-auto=validate`; H2 em memória apenas no perfil `test`

**Testing**: JUnit 5, Mockito, AssertJ, `spring-boot-starter-webmvc-test` (`@WebMvcTest` + `jwt()` de spring-security-test), `spring-boot-starter-data-jpa-test`, `@SpringBootTest` + MockMvc com Keycloak real via `testcontainers-keycloak` 4.3.1 (imagem `quay.io/keycloak/keycloak:26.7.4`); JaCoCo 0.8.15 `check` 100% linhas/ramos; Surefire executa `*Test` e `*IT`

**Target Platform**: Servidor Linux em container (`eclipse-temurin:21-jre-alpine`, usuário não-root); desenvolvimento em Windows 11 com Docker Desktop

**Project Type**: web-service (API REST única, sem frontend nesta feature)

**Performance Goals**: listagem completa de até 500 tarefas em < 1 s percebido (SC-006); demais operações < 300 ms p95 em uso pessoal

**Constraints**: todo código, mensagens, logs, Swagger e docs em português (Princípio II); nenhuma credencial em código; posições 1..N sempre contínuas; "última gravação prevalece" (sem `@Version`); fuso `America/Sao_Paulo`; lista sem paginação

**Scale/Scope**: uso pessoal — dezenas de usuários, centenas de tarefas por usuário, milhares de registros de histórico; 10 endpoints REST; 2 entidades

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Avaliado contra `.specify/memory/constitution.md` v1.0.0.

| # | Princípio | Portão | Status |
|---|-----------|--------|--------|
| I | Arquitetura em Camadas | Pacotes `controller`/`service`/`repository`/`dto`/`entity`/`mapper`/`config`/`exception` (+ `seguranca`, `util` como apoio); entidades nunca expostas — controllers só falam DTO; `TarefaService` orquestra repositórios; API stateless (JWT, sem sessão); `TratadorGlobalExcecoes` com `ProblemDetail` | ✅ |
| II | Idioma Português | Todas as classes, métodos, DTOs, mensagens de validação, `@Operation`/`@Schema`, logs, migração SQL, testes (`deveRetornar404QuandoTarefaNaoExistir`) e documentos desta feature em português; exceções apenas para identificadores de framework | ✅ |
| III | Testes Primeiro / 100% | Cada tarefa de implementação precedida por testes (unidade por camada + IT por endpoint); JaCoCo `check` LINE/BRANCH = 1.00 em `verify`; exclusões limitadas a `TaskBackendApplication` e `mapper/*Impl` (código gerado), justificadas no `pom.xml`; `lombok.addLombokGeneratedAnnotation=true`; `Clock` injetável garante determinismo | ✅ |
| IV | Segurança Keycloak | Resource Server JWT (`issuer-uri` + `jwk-set-uri`); `realm_access.roles` → `ROLE_*`; `/api/**` exige `hasRole("usuario")`; públicos apenas `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`; nenhuma tabela de usuários ou senha; cadastro de usuários feito no Keycloak (fora do escopo da API) | ✅ |
| V | Contrato OpenAPI | springdoc 3.1.1, Swagger UI em `/swagger-ui.html`, `bearerAuth` declarado; contrato de referência em `contracts/openapi.yaml`; caminho versionado `/api/v1` | ✅ |
| VI | Persistência H2 + Flyway | H2 em arquivo fora dos testes; `V1__*.sql` versionada; `ddl-auto=validate`; console H2 só no perfil `dev`; volume Docker `dados-h2` | ✅ |
| VII | Docker | `Dockerfile` multi-stage não-root + `compose.yaml` (app + Keycloak com `--import-realm`, healthcheck, volume); configuração por variáveis de ambiente; `docker compose up --build` sem passos manuais | ✅ |
| VIII | Simplicidade / Lombok | DTOs `record`; entidades com `@Getter/@Setter/@Builder`, sem `@Data`; injeção por construtor via `@RequiredArgsConstructor`; sem interfaces de serviço desnecessárias; pacotes extras (`seguranca`, `util`) são utilitários coesos, não camadas novas | ✅ |

**Resultado pré-Fase 0**: todos os portões ✅ — sem violações a justificar.

**Re-avaliação pós-Fase 1 (design)**: mantidos ✅. Pontos verificados no design:
- `RegistroHistorico` gravado pelo `TarefaService` no mesmo `@Transactional` — não cria
  camada nova nem acoplamento cruzado (I, VIII).
- `*MapperImpl` excluído do JaCoCo é código gerado; a interface `TarefaMapper` é
  exercitada por `TarefaMapperTest` (III).
- `spring-boot-h2console` em `runtime` com `spring.h2.console.enabled` somente em
  `application-dev.yml` (VI).
- Nenhuma dependência fora das listadas em `research.md` (Stack).

## Project Structure

### Documentation (this feature)

```text
specs/001-gerenciamento-tarefas-api/
├── plan.md              # Este arquivo (/speckit-plan)
├── research.md          # Fase 0 — decisões R-01..R-16
├── data-model.md        # Fase 1 — entidades, enums, migração, consultas, DTOs
├── quickstart.md        # Fase 1 — validação ponta a ponta
├── contracts/
│   └── openapi.yaml     # Fase 1 — contrato REST de referência
├── checklists/
│   └── requirements.md  # Qualidade da spec (completo)
└── tasks.md             # Fase 2 — gerado por /speckit-tasks (NÃO por este comando)
```

### Source Code (repository root)

```text
pom.xml                                   # deps Boot 4.1.1, springdoc, MapStruct, JaCoCo 100%
lombok.config                             # addLombokGeneratedAnnotation, stopBubbling
Dockerfile                                # multi-stage maven → temurin 21 jre alpine, non-root
compose.yaml                              # app (8080) + keycloak (8180→8080) + volume dados-h2
keycloak/
└── realm-export.json                     # realm tarefas, role usuario, clientes, usuários dev
dados/                                    # H2 em arquivo (gitignored)

src/main/java/com/task/task_backend/
├── TaskBackendApplication.java
├── config/
│   ├── SegurancaConfig.java              # SecurityFilterChain, conversor de roles, 401/403 ProblemDetail
│   ├── OpenApiConfig.java                # bean OpenAPI (título, bearerAuth, descrição pt-BR)
│   └── RelogioConfig.java                # bean Clock America/Sao_Paulo
├── controller/
│   └── TarefaController.java             # 10 endpoints /api/v1/tarefas
├── dto/
│   ├── TarefaRequisicaoDTO.java
│   ├── TarefaRespostaDTO.java
│   ├── MoverTarefaRequisicaoDTO.java
│   ├── RegistroHistoricoRespostaDTO.java
│   ├── ResumoFinanceiroRespostaDTO.java
│   └── ErroValidacaoDTO.java
├── entity/
│   ├── Tarefa.java
│   ├── RegistroHistorico.java
│   ├── Prioridade.java
│   ├── Situacao.java
│   ├── TipoRegistro.java
│   ├── DestinoMovimento.java
│   └── FiltroSituacao.java
├── exception/
│   ├── TratadorGlobalExcecoes.java       # @RestControllerAdvice → ProblemDetail
│   ├── TarefaNaoEncontradaException.java # 404
│   └── PosicaoInvalidaException.java     # 400
├── mapper/
│   ├── TarefaMapper.java                 # MapStruct, calcula atrasada via Clock
│   └── RegistroHistoricoMapper.java
├── repository/
│   ├── TarefaRepository.java
│   └── RegistroHistoricoRepository.java
├── seguranca/
│   ├── UsuarioLogado.java                # record(id, nome)
│   ├── UsuarioAutenticado.java           # lê Jwt do SecurityContext
│   └── ConversorAutoridadesKeycloak.java # realm_access.roles → ROLE_*
├── service/
│   ├── TarefaService.java                # CRUD, concluir/reabrir, mover, excluir, listar
│   ├── HistoricoService.java             # diff campo a campo + gravação
│   └── ResumoFinanceiroService.java      # soma do mês corrente
└── util/
    ├── NormalizadorTexto.java            # remove acentos + lowercase
    └── FormatadorHistorico.java          # R$ 1.250,00 / dd/MM/yyyy / rótulos

src/main/resources/
├── application.yml                       # comum: jpa validate, flyway, springdoc, actuator
├── application-dev.yml                   # H2 arquivo ./dados, console H2, keycloak localhost:8180
├── application-docker.yml                # H2 /app/dados, issuer localhost:8180 + jwk keycloak:8080
└── db/migration/
    └── V1__criar_tabelas_tarefa_e_historico.sql

src/test/java/com/task/task_backend/
├── unit/
│   ├── controller/TarefaControllerTest.java          # @WebMvcTest + jwt()
│   ├── service/TarefaServiceTest.java
│   ├── service/HistoricoServiceTest.java
│   ├── service/ResumoFinanceiroServiceTest.java
│   ├── mapper/TarefaMapperTest.java
│   ├── mapper/RegistroHistoricoMapperTest.java
│   ├── seguranca/UsuarioAutenticadoTest.java
│   ├── seguranca/ConversorAutoridadesKeycloakTest.java
│   ├── exception/TratadorGlobalExcecoesTest.java
│   ├── entity/TarefaTest.java                        # @PrePersist normalização, enums rótulos
│   ├── util/NormalizadorTextoTest.java
│   └── util/FormatadorHistoricoTest.java
├── integration/
│   ├── KeycloakContainerSuporte.java                 # container compartilhado + obterToken(usuario)
│   ├── TarefaRepositoryIT.java                       # @DataJpaTest + Flyway: shifts, busca, soma
│   ├── TarefaCrudIT.java                             # US1, US2, US5
│   ├── TarefaReordenacaoIT.java                      # US3 (inclui propriedade 1..N)
│   ├── TarefaHistoricoIT.java                        # US4
│   ├── TarefaFiltroBuscaFinanceiroIT.java            # US6
│   ├── SegurancaIT.java                              # 401/403/isolamento entre usuários
│   └── OpenApiIT.java                                # /v3/api-docs e swagger-ui acessíveis
└── TaskBackendApplicationTests.java                  # contextLoads (perfil test)

src/test/resources/
├── application-test.yml                              # H2 mem, flyway, issuer via DynamicPropertySource
└── keycloak/realm-export.json → (symlink não; copiado do keycloak/ no build via resources plugin)
```

**Structure Decision**: projeto único Spring Boot/Maven (Opção 1 do template, layout
padrão da constituição). Pacotes `seguranca` e `util` foram adicionados às camadas
canônicas como utilitários coesos — não introduzem camada arquitetural nova. Testes
espelham os pacotes de produção em `unit/` e `integration/`. O `realm-export.json` é
único (em `keycloak/`) e copiado para o classpath de teste pelo
`maven-resources-plugin` para que Compose e Testcontainers usem exatamente o mesmo realm.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Nenhuma violação. Tabela não aplicável.

## Fases de Implementação (visão para `/speckit-tasks`)

1. **Setup**: `pom.xml` completo (deps, annotation processors, Surefire includes,
   JaCoCo 100%), `lombok.config`, `application*.yml`, `.gitignore` (`dados/`),
   `keycloak/realm-export.json`, `Dockerfile`, `compose.yaml`.
2. **Fundação**: migração V1, entidades e enums, `RelogioConfig`, `SegurancaConfig` +
   `seguranca/*`, `OpenApiConfig`, `TratadorGlobalExcecoes`, utilitários, suporte de
   Testcontainers (`KeycloakContainerSuporte`).
3. **US1 + US2 (P1)**: DTOs, mappers, repositório, `TarefaService` (criar, listar,
   obter, editar, concluir, reabrir), `HistoricoService` (criação/alteração/situação),
   controller correspondente — testes primeiro.
4. **US3 (P2)**: mover (TOPO/FINAL/POSICAO), shifts, lock pessimista, validação de
   intervalo; teste de propriedade 1..N.
5. **US4 (P2)**: endpoint de histórico, `FormatadorHistorico`, frase pronta.
6. **US5 (P3)**: exclusão com reorganização e cascata.
7. **US6 (P3)**: filtro, busca normalizada, `ResumoFinanceiroService`.
8. **Polimento**: cobertura 100% (ramos restantes), Swagger revisado em português,
   `./mvnw verify` + `docker compose up --build`, execução do `quickstart.md`.
