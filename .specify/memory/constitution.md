<!--
Sync Impact Report
==================
Version change: (template sem versão) → 1.0.0
Bump rationale: ratificação inicial — todos os placeholders preenchidos, 8 princípios
e 2 seções complementares definidos a partir da entrada do usuário e do pom.xml existente.

Modified principles: N/A (primeira ratificação)

Added sections:
- Princípios Fundamentais (I–VIII)
- Stack Tecnológica e Restrições
- Fluxo de Desenvolvimento e Portões de Qualidade
- Governança

Removed sections: nenhuma

Templates requiring updates:
- ✅ .specify/templates/plan-template.md — "Constitution Check" com portões concretos
- ✅ .specify/templates/spec-template.md — nota de idioma (português) adicionada
- ✅ .specify/templates/tasks-template.md — testes tornados OBRIGATÓRIOS, convenções
     de caminho Java/Maven, tarefas de cobertura/JaCoCo, Swagger e Docker
- ⚠ .specify/templates/commands/*.md — diretório inexistente neste projeto (skills
     residem em .claude/skills/*/SKILL.md e são genéricas; nenhuma alteração necessária)
- ⚠ CLAUDE.md — apenas delega ao plano atual; sem referências a princípios a atualizar

Follow-up TODOs: nenhum placeholder deferido.
Interpretação registrada: o termo "ephemeral" da entrada do usuário foi interpretado
como serviços sem estado (stateless) e infraestrutura de teste efêmera (Testcontainers).
Ver Princípio I e Princípio III.
-->

# Constituição do Task Backend

## Princípios Fundamentais

### I. Arquitetura em Camadas com Serviços Sem Estado

O código de produção DEVE ser organizado nas camadas `controller`, `service`,
`repository`, `dto`, `entity` (ou `dominio`), `mapper`, `config` e `exception`,
abaixo do pacote raiz `com.task.task_backend`.

- Controllers DEVEM apenas receber requisições, validar entrada (`@Valid`) e delegar
  a um Service; NÃO PODEM conter regra de negócio nem acessar Repositories.
- Services DEVEM concentrar a regra de negócio e ser a única camada a orquestrar
  Repositories e chamadas externas.
- Repositories DEVEM ser interfaces Spring Data JPA; consultas customizadas DEVEM ser
  declaradas com `@Query` ou métodos derivados, nunca com SQL espalhado em Services.
- Entidades JPA NUNCA PODEM ser expostas em endpoints; toda entrada e saída HTTP DEVE
  usar DTOs (`record` Java) convertidos por Mappers dedicados.
- Serviços DEVEM ser efêmeros/sem estado: nenhum estado de usuário em memória ou sessão
  HTTP; toda autenticação é stateless via token (ver Princípio IV).
- Exceções de negócio DEVEM ser tratadas centralmente por um `@RestControllerAdvice`
  retornando `ProblemDetail` (RFC 9457).

**Justificativa**: separação clara de responsabilidades permite testes unitários
isolados por camada, facilita a cobertura total (Princípio III) e habilita escala
horizontal em containers (Princípio VII).

### II. Idioma Português em Todo o Projeto

Todo artefato produzido DEVE estar em português do Brasil:

- Nomes de classes, métodos, variáveis, pacotes de domínio, DTOs, testes e constantes
  (ex.: `TarefaService`, `criarTarefa`, `TarefaRespostaDTO`).
- Comentários, Javadoc, mensagens de log, mensagens de erro e validação
  (`@NotBlank(message = "O título é obrigatório")`).
- Descrições Swagger/OpenAPI (`@Operation(summary = "Cria uma nova tarefa")`).
- Documentação (`README`, specs, planos, tarefas) e mensagens de commit.
- Toda comunicação do agente/assistente com o usuário via terminal (CLI) DEVE ser em
  português.

Exceções permitidas: identificadores impostos por frameworks/bibliotecas (ex.:
`toString`, `equals`, anotações Spring), nomes de tecnologias e termos técnicos sem
tradução consagrada (ex.: `token`, `endpoint`, `DTO`).

**Justificativa**: o time e os stakeholders são lusófonos; um idioma único elimina
ambiguidade entre código, documentação e conversa.

### III. Testes Primeiro com Cobertura Total (NÃO NEGOCIÁVEL)

- TDD é obrigatório: os testes DEVEM ser escritos antes da implementação, DEVEM falhar
  primeiro e só então o código de produção é escrito (Red-Green-Refactor).
- Toda classe de produção DEVE possuir testes de unidade (JUnit 5 + Mockito) que
  exercitem todos os ramos.
- Todo endpoint e toda integração com banco/Keycloak DEVE possuir testes de integração
  (`@SpringBootTest` + `MockMvc`/`WebTestClient`), executando contra H2 e Keycloak
  efêmero (Testcontainers) ou segurança simulada com `spring-security-test`.
- A cobertura JaCoCo DEVE ser 100% em linhas e ramos; o build (`mvn verify`) DEVE
  falhar abaixo desse limiar. Exclusões são permitidas SOMENTE para a classe
  `TaskBackendApplication`, classes geradas (MapStruct, Lombok) e configurações
  puramente declarativas, e cada exclusão DEVE ser justificada em comentário no
  `pom.xml`.
- Nomes de testes DEVEM descrever o comportamento em português
  (`deveRetornar404QuandoTarefaNaoExistir`).

**Justificativa**: cobertura total, combinada com TDD, é o portão de qualidade que
sustenta refatorações seguras e é requisito explícito do produto.

### IV. Segurança Delegada ao Keycloak

- Autenticação, login, cadastro e gestão de usuários DEVEM ser delegados ao Keycloak;
  a aplicação NUNCA PODE armazenar senhas ou implementar fluxo próprio de login.
- A API DEVE atuar como OAuth2 Resource Server validando JWT emitidos pelo Keycloak
  (`spring-boot-starter-oauth2-resource-server`).
- Autorização DEVE ser baseada em roles/claims do token mapeadas para
  `GrantedAuthority`, aplicadas via `@PreAuthorize` ou regras no `SecurityFilterChain`.
- Cadastro de usuários exposto pela API (quando exigido pela feature) DEVE ser feito via
  Keycloak Admin REST API/Admin Client, nunca por tabela local de usuários.
- Endpoints públicos DEVEM ser explicitamente listados (Swagger UI, `/actuator/health`);
  todo o restante é negado por padrão.

**Justificativa**: delegar identidade a um IdP maduro reduz superfície de ataque e
elimina a necessidade de reinventar segurança.

### V. Contrato de API Documentado via OpenAPI

- Toda API DEVE ser documentada com springdoc-openapi; a Swagger UI DEVE estar acessível
  em `/swagger-ui.html` e o contrato em `/v3/api-docs`.
- Todo endpoint DEVE declarar `@Operation`, `@ApiResponses` (incluindo códigos de erro)
  e todo DTO DEVE ter `@Schema` com descrição e exemplo em português.
- O esquema de segurança (`bearerAuth`/OAuth2) DEVE estar declarado na configuração
  OpenAPI para permitir testes autenticados pela UI.
- Mudanças incompatíveis no contrato DEVEM ser versionadas no caminho (`/api/v1/...`).

**Justificativa**: o contrato gerado é a fonte de verdade para consumidores e para os
testes de contrato/integração.

### VI. Persistência H2 em Modo Arquivo com Migrações Versionadas

- O banco DEVE ser H2 em modo persistente (`jdbc:h2:file:./dados/taskdb` ou caminho
  montado em volume Docker), nunca em memória fora dos testes.
- O esquema DEVE ser gerido por migrações Flyway versionadas
  (`src/main/resources/db/migration/V{n}__descricao.sql`); `spring.jpa.hibernate.ddl-auto`
  DEVE ser `validate` em execução normal.
- Testes de integração PODEM usar H2 em memória via perfil `test`, mantendo as mesmas
  migrações Flyway para garantir paridade de esquema.
- O console H2 SÓ PODE estar habilitado nos perfis `dev`/`local`.

**Justificativa**: persistência em arquivo atende ao requisito de dados duráveis sem
custo operacional; migrações versionadas tornam o esquema reprodutível e testável.

### VII. Entrega Containerizada com Docker

- O projeto DEVE conter `Dockerfile` multi-stage (build Maven + runtime JRE 21 slim,
  usuário não-root) e `compose.yaml` orquestrando a aplicação e o Keycloak (com realm
  importado automaticamente a partir de `keycloak/realm-export.json`).
- Toda configuração sensível ou dependente de ambiente DEVE ser externalizada via
  variáveis de ambiente/`application-{perfil}.yml`; nenhum segredo em código.
- O volume do H2 DEVE ser declarado no compose para garantir a persistência
  (Princípio VI).
- `docker compose up` DEVE resultar em ambiente funcional sem passos manuais adicionais.

**Justificativa**: containers garantem paridade dev/prod e simplificam a subida do
Keycloak, que é dependência obrigatória para qualquer teste manual da API.

### VIII. Simplicidade e Redução de Boilerplate com Lombok

- Lombok DEVE ser usado para eliminar boilerplate em entidades e componentes
  (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`);
  `@Data` NÃO PODE ser usado em entidades JPA (risco em `equals`/`hashCode`).
- DTOs DEVEM ser `record` Java 21; recursos modernos da linguagem (pattern matching,
  `sealed`, `switch` com padrões, `var` local) SÃO preferidos quando aumentam clareza.
- Injeção de dependência DEVE ser por construtor (`@RequiredArgsConstructor` + campos
  `final`); `@Autowired` em campo é proibido.
- YAGNI: abstrações, interfaces e camadas adicionais além das definidas no Princípio I
  SÓ PODEM ser introduzidas com justificativa registrada na seção "Complexity Tracking"
  do plano.

**Justificativa**: menos código gerado à mão significa menos linhas a cobrir e menos
pontos de falha, reforçando o Princípio III.

## Stack Tecnológica e Restrições

| Área | Tecnologia obrigatória |
|------|------------------------|
| Linguagem | Java 21 (LTS) — `<java.version>21</java.version>` |
| Framework | Spring Boot 4.1.x (parent já definido no `pom.xml`) |
| Build | Maven Wrapper (`./mvnw`); nenhum comando exige Maven global |
| Web | Spring Web MVC (`spring-boot-starter-web`) + Bean Validation |
| Persistência | Spring Data JPA + H2 (modo arquivo) + Flyway |
| Segurança | Spring Security OAuth2 Resource Server + Keycloak (JWT) |
| Documentação | springdoc-openapi (versão compatível com Spring Boot 4) |
| Boilerplate | Lombok; MapStruct para mapeamento entidade ↔ DTO |
| Testes | JUnit 5, Mockito, AssertJ, `spring-security-test`, Testcontainers (Keycloak) |
| Cobertura | JaCoCo com `check` de 100% linhas/ramos na fase `verify` |
| Containers | Docker (Dockerfile multi-stage) + Docker Compose (app + Keycloak) |
| Observabilidade | Spring Boot Actuator (`/actuator/health`) e logs estruturados via SLF4J |

Restrições adicionais:

- Nenhuma dependência nova PODE ser adicionada sem menção no `research.md` da feature.
- Versões DEVEM ser gerenciadas pelo BOM do Spring Boot; versões explícitas só para
  bibliotecas fora do BOM (springdoc, MapStruct, Testcontainers Keycloak).
- Estrutura de código-fonte padrão Maven: `src/main/java`, `src/main/resources`,
  `src/test/java`, `src/test/resources`.

## Fluxo de Desenvolvimento e Portões de Qualidade

1. **Especificação → Plano → Tarefas → Implementação**: toda feature segue o fluxo
   Spec Kit (`/speckit-specify`, `/speckit-plan`, `/speckit-tasks`,
   `/speckit-implement`); o plano DEVE conter a seção "Constitution Check" avaliando
   cada princípio desta constituição.
2. **Red-Green-Refactor por tarefa**: cada tarefa de implementação é precedida pela
   tarefa de teste correspondente; testes DEVEM falhar antes do código de produção.
3. **Portão de build**: `./mvnw verify` DEVE passar localmente antes de qualquer commit,
   incluindo compilação, testes de unidade, testes de integração e `jacoco:check` a 100%.
4. **Portão de contrato**: após alteração de endpoint, a Swagger UI DEVE ser verificada
   e o `research.md`/`contracts/` da feature atualizados.
5. **Portão de container**: alterações em configuração, dependências ou perfis DEVEM
   ser validadas com `docker compose up --build` antes da conclusão da feature.
6. **Revisão**: todo PR/revisão DEVE verificar explicitamente conformidade com os
   Princípios I–VIII; violações DEVEM constar em "Complexity Tracking" com justificativa
   ou ser corrigidas.
7. **Commits**: mensagens em português, no formato Conventional Commits
   (`feat:`, `fix:`, `test:`, `docs:`, `chore:`), pequenas e atômicas.

## Governança

- Esta constituição prevalece sobre qualquer outra prática, template ou convenção do
  repositório. Em caso de conflito, a constituição vence e o artefato conflitante DEVE
  ser corrigido.
- **Emendas**: qualquer alteração DEVE ser feita via `/speckit-constitution`,
  documentada no Sync Impact Report no topo deste arquivo, propagada aos templates
  dependentes (`plan-template.md`, `spec-template.md`, `tasks-template.md`) e
  registrada em commit próprio (`docs: emenda da constituição para vX.Y.Z`).
- **Versionamento semântico**:
  - MAJOR — remoção ou redefinição incompatível de princípio ou regra de governança;
  - MINOR — novo princípio/seção ou expansão material de orientação existente;
  - PATCH — esclarecimentos, correções de redação ou refinamentos não semânticos.
- **Revisão de conformidade**: o "Constitution Check" de cada plano é o ponto de
  auditoria obrigatório; o `/speckit-analyze` DEVE ser executado antes de
  `/speckit-implement` para detectar violações entre spec, plano e tarefas.
- **Orientação em tempo de execução**: `CLAUDE.md` e o plano ativo da feature
  (`specs/<feature>/plan.md`) fornecem o contexto operacional; ambos DEVEM referenciar
  esta constituição como fonte de verdade.

**Version**: 1.0.0 | **Ratified**: 2026-09-18 | **Last Amended**: 2026-09-18
