# Pesquisa Técnica: API de Gerenciamento de Tarefas

**Feature**: `001-gerenciamento-tarefas-api` | **Data**: 2026-09-19
**Objetivo**: resolver todas as incertezas do Contexto Técnico do plano e registrar as
decisões com justificativa e alternativas avaliadas.

Ambiente verificado em 2026-09-19: Java 21.0.6 (Oracle), Maven Wrapper 3.9.16,
Docker 27.5.1, locale `pt_BR`, Spring Boot 4.1.1 já definido como parent no `pom.xml`.

---

## R-01. Starters do Spring Boot 4.1.1 (nomes modularizados)

**Decisão**: usar os novos artefatos modulares do Boot 4 confirmados no Maven Central:

| Finalidade | Artefato | Escopo |
|-----------|----------|--------|
| Web MVC + Jackson | `spring-boot-starter-webmvc` | compile |
| Bean Validation | `spring-boot-starter-validation` | compile |
| JPA/Hibernate | `spring-boot-starter-data-jpa` | compile |
| Migrações | `spring-boot-starter-flyway` (novo no Boot 4) | compile |
| Segurança JWT | `spring-boot-starter-oauth2-resource-server` | compile |
| Saúde | `spring-boot-starter-actuator` | compile |
| Cliente HTTP (feature 002) | `spring-boot-starter-restclient` — no Boot 4 o `RestClient.Builder` autoconfigurado vive neste starter | compile |
| Banco | `com.h2database:h2` (versão via BOM, 2.x) | runtime |
| Console H2 (dev) | `spring-boot-h2console` | runtime |
| Testes base | `spring-boot-starter-test` | test |
| Testes MVC slice | `spring-boot-starter-webmvc-test` | test |
| Testes segurança | `spring-boot-starter-security-test` | test |
| Testes JPA slice | `spring-boot-starter-data-jpa-test` | test |
| Testcontainers | `spring-boot-testcontainers` + `org.testcontainers:testcontainers-junit-jupiter` (Testcontainers 2.0.5 via BOM do Boot; o módulo `junit-jupiter` foi renomeado na linha 2.x) | test |

**Justificativa**: Boot 4 dividiu os starters; `spring-boot-starter-web` ainda existe
como alias, mas `spring-boot-starter-webmvc` é o nome canônico. O starter Flyway elimina
a configuração manual que era necessária no Boot 3.
**Alternativas**: manter nomes do Boot 3 (`starter-web`) — rejeitado por ser legado.

## R-02. Documentação OpenAPI

**Decisão**: `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1`.
**Justificativa**: linha 3.x é a compatível com Spring Boot 4 / Spring Framework 7
(a linha 2.x é para Boot 3). Versão mais recente publicada em 2026-09.
**Configuração**: `springdoc.swagger-ui.path=/swagger-ui.html`, esquema de segurança
`bearerAuth` (HTTP bearer JWT) declarado via bean `OpenAPI`; todas as descrições em
português (`@Operation`, `@ApiResponse`, `@Schema`).
**Alternativas**: Springfox — abandonado; documentação manual — viola Princípio V.

## R-03. Mapeamento entidade ↔ DTO

**Decisão**: MapStruct `1.6.3` + `lombok-mapstruct-binding:0.2.0`, com
`annotationProcessorPaths` na ordem Lombok → binding → MapStruct no
`maven-compiler-plugin`. `componentModel = "spring"`.
**Justificativa**: exigência da constituição (Stack). O binding garante que MapStruct
veja getters/setters gerados pelo Lombok.
**Cobertura**: classes `*MapperImpl` geradas são excluídas do JaCoCo (código gerado,
permitido pela constituição §III) — a interface do mapper é exercitada pelos testes.
**Alternativas**: mapeamento manual em `record` — mais linhas para cobrir, rejeitado.

## R-04. Cobertura 100% com JaCoCo

**Decisão**: `jacoco-maven-plugin:0.8.15` com `prepare-agent` + `report` + `check`
(regras `LINE` e `BRANCH` = 1.00) ligado à fase `verify`.
Exclusões justificadas (comentadas no `pom.xml`):
- `com/task/task_backend/TaskBackendApplication.class` — bootstrap;
- `com/task/task_backend/mapper/*Impl.class` — código gerado pelo MapStruct.
Arquivo `lombok.config` na raiz com `lombok.addLombokGeneratedAnnotation = true` para
que JaCoCo ignore automaticamente o código gerado pelo Lombok
(`config.stopBubbling = true`).
**Testes unitários e de integração no mesmo ciclo**: Surefire executa `**/*Test.java`
e `**/*IT.java` (includes explícitos), gerando um único `jacoco.exec`. Evita a
complexidade de Failsafe + merge de relatórios.
**Alternativas**: Failsafe separado — rejeitado (dois agentes JaCoCo + `merge`, mais
pontos de falha para o mesmo resultado).

## R-05. Estratégia de testes de segurança

**Decisão em dois níveis**:
1. **Unidade / slice** (`@WebMvcTest`): autenticação simulada com
   `SecurityMockMvcRequestPostProcessors.jwt()` de `spring-security-test`, injetando
   claims `sub`, `preferred_username`, `name` e `realm_access.roles`.
2. **Integração** (`@SpringBootTest` + `MockMvc` pleno): Keycloak real efêmero via
   `com.github.dasniko:testcontainers-keycloak:4.3.1` (imagem
   `quay.io/keycloak/keycloak:26.7.4`) com o mesmo `realm-export.json` usado no
   Docker Compose; tokens obtidos por *password grant* no cliente de teste;
   `issuer-uri` injetado com `@DynamicPropertySource`.
**Justificativa**: o nível 1 é rápido e cobre 100% dos ramos de controller/segurança;
o nível 2 prova a integração real com Keycloak (Princípio III exige integração real).
Docker está disponível na máquina de desenvolvimento.
**Alternativas**: apenas `jwt()` — não valida a integração real; Keycloak em
`compose` durante testes — acoplamento ao ambiente, rejeitado.

## R-06. Contexto do usuário autenticado

**Decisão**: componente `UsuarioAutenticado` (record `UsuarioLogado(id, nome)`) que lê
o `Jwt` do `SecurityContext`: `id = sub`; `nome = name`, senão `preferred_username`,
senão `sub` (cobre o edge case "usuário sem nome de exibição").
Roles: `JwtAuthenticationConverter` customizado extraindo `realm_access.roles` e
prefixando `ROLE_`; rotas `/api/**` exigem `hasRole("usuario")`.
**Justificativa**: Princípio IV (roles mapeadas para `GrantedAuthority`); realm concede
`usuario` como *default role* a todo usuário.

## R-07. Persistência H2 persistente + Flyway

**Decisão**:
- dev: `jdbc:h2:file:./dados/tarefas;MODE=LEGACY=FALSE` → `spring.datasource.url=jdbc:h2:file:./dados/tarefas`;
- docker: `jdbc:h2:file:/app/dados/tarefas` com volume nomeado `dados-h2`;
- test: `jdbc:h2:mem:tarefas;DB_CLOSE_DELAY=-1`.
- `spring.jpa.hibernate.ddl-auto=validate` em todos os perfis; `spring.jpa.open-in-view=false`.
- Migração `V1__criar_tabelas_tarefa_e_historico.sql`.
- `dados/` adicionado ao `.gitignore`.
- Console H2 (`spring.h2.console.enabled=true`) somente no perfil `dev`.
**Alternativas**: `ddl-auto=update` — viola Princípio VI.

## R-08. Algoritmo de reordenação (posições contínuas 1..N)

**Decisão**: posição inteira por usuário, sem *gaps*. Em uma única transação:
- mover `de → para` com `para < de`: `UPDATE tarefa SET posicao = posicao + 1 WHERE usuario_id = ? AND posicao BETWEEN para AND de-1`;
- `para > de`: `UPDATE ... posicao = posicao - 1 WHERE posicao BETWEEN de+1 AND para`;
- depois `SET posicao = para` na tarefa movida. `TOPO` = 1; `FINAL` = N; `POSICAO` = valor informado (validado em 1..N, erro 400 fora do intervalo);
- exclusão: `UPDATE ... posicao = posicao - 1 WHERE posicao > excluida`;
- criação: `posicao = COUNT(*)+1`.
Sem `UNIQUE(usuario_id, posicao)` no banco (o shift em lote violaria a restrição
transitoriamente no H2); integridade garantida pelo serviço e por testes de
propriedade (FR-014). Índice não único `(usuario_id, posicao)`.
Concorrência: `@Transactional` com `SELECT ... FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`)
na tarefa movida para serializar reordenações do mesmo usuário; edições seguem
"última gravação prevalece" (spec), portanto **sem** `@Version`.
**Alternativas**: posição fracionária/float — evita shifts mas gera *gaps* e viola
FR-014; lista ligada — complexa demais para o volume.

## R-09. Busca sem acentos e sem distinção de caixa

**Decisão**: coluna `descricao_normalizada` preenchida em `@PrePersist/@PreUpdate` via
`Normalizer.normalize(texto, NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)`;
o termo de busca passa pela mesma função utilitária (`NormalizadorTexto`) e a consulta
usa `LIKE CONCAT('%', :termo, '%')` sobre a coluna normalizada.
**Justificativa**: H2 não possui `unaccent`; normalizar na escrita mantém a consulta
simples e indexável.
**Alternativas**: função H2 customizada em Java — acopla ao banco; rejeitada.

## R-10. Histórico de alterações

**Decisão**: entidade `RegistroHistorico` (imutável) gravada pelo `TarefaService` no
mesmo `@Transactional` da operação. O serviço compara campo a campo (descrição,
prioridade, data prevista, valor, situação) e cria um registro por campo alterado; sem
alteração → nenhum registro; reordenação → nenhum registro (FR-017).
Tipos: `CRIACAO`, `ALTERACAO`, `CONCLUSAO`, `REABERTURA`.
Formatação para a interface (FR-019): valores `R$ 1.250,00` via `DecimalFormat`
com `DecimalFormatSymbols(pt-BR)` prefixado por `"R$ "` (evita o NBSP que
`NumberFormat.getCurrencyInstance` insere); datas `dd/MM/yyyy`; prioridade/situação
com rótulos em português (`Alta`, `Concluída`). Utilitário `FormatadorHistorico`.
`ON DELETE CASCADE` na FK para remover o histórico junto com a tarefa (FR-020).
**Alternativas**: Hibernate Envers — gera tabelas e formato genéricos, não atende o
texto "Alterou valor de R$50 para R$70"; rejeitado.

## R-11. Datas, fuso e relógio testável

**Decisão**: `Clock` como bean (`Clock.system(ZoneId.of("America/Sao_Paulo"))`)
injetado nos serviços; testes substituem por `Clock.fixed`. "Mês corrente" e
"atrasada" calculados com `LocalDate.now(clock)`. Persistência: `data_abertura` e
`data_conclusao_real` como `TIMESTAMP` (`LocalDateTime`); `data_prevista_conclusao` como
`DATE` (`LocalDate`).
**Justificativa**: determinismo nos testes (Princípio III) e fuso brasileiro (spec).

## R-12. Tratamento de erros e mensagens em português

**Decisão**: `TratadorGlobalExcecoes extends ResponseEntityExceptionHandler`
(`@RestControllerAdvice`) retornando `ProblemDetail` (RFC 9457) com `title`/`detail`
em português e, para validação, propriedade `erros: [{campo, mensagem}]`.
Exceções de domínio: `TarefaNaoEncontradaException` (404),
`PosicaoInvalidaException` (400). 401/403 tratados por `AuthenticationEntryPoint` e
`AccessDeniedHandler` customizados que também emitem `ProblemDetail` em português.
Mensagens Bean Validation explícitas em cada anotação (`message = "..."`) — evita
depender de bundle e de `Accept-Language`.
**Alternativas**: `Accept-Language` + `messages_pt_BR.properties` — mais indireto; a
API é monolíngue por constituição.

## R-13. Contrato REST

**Decisão** (detalhado em `contracts/openapi.yaml`), base `/api/v1`:

| Método | Caminho | Operação |
|--------|---------|----------|
| GET | `/tarefas?situacao=&busca=` | listar (lista completa, filtro + busca) |
| POST | `/tarefas` | criar (201 + `Location`) |
| GET | `/tarefas/{id}` | obter |
| PUT | `/tarefas/{id}` | editar campos de conteúdo |
| PATCH | `/tarefas/{id}/concluir` | concluir |
| PATCH | `/tarefas/{id}/reabrir` | reabrir |
| PATCH | `/tarefas/{id}/mover` | mover (`destino`: `TOPO`/`FINAL`/`POSICAO`) |
| DELETE | `/tarefas/{id}` | excluir (204) |
| GET | `/tarefas/{id}/historico` | histórico (mais recente primeiro) |
| GET | `/tarefas/resumo-financeiro` | total a gastar no mês corrente |

`PUT` (não `PATCH`) para edição: o formulário unificado do front envia sempre todos os
campos editáveis; simplifica validação e diff de histórico.
**Alternativas**: endpoints separados `/topo` e `/final` — três controllers para a mesma
regra; enum `destino` mantém um único ponto de validação.

## R-14. Keycloak: realm, clientes e Docker Compose

**Decisão**:
- Imagem `quay.io/keycloak/keycloak:26.7.4`, comando `start-dev --import-realm`,
  realm `tarefas` em `keycloak/realm-export.json` (versionado): role de realm
  `usuario` (default role), cliente público `tarefas-frontend` (PKCE, para o futuro
  front), cliente público `tarefas-teste` com *Direct Access Grants* habilitado (usado
  apenas por testes/Swagger para obter token por login e senha), usuários de exemplo
  `ana` e `bruno` (senha `senha123`) — somente em desenvolvimento.
- Portas: aplicação `8080`, Keycloak `8180` (host) → `8080` (container).
- Emissor: tokens obtidos pelo navegador/Swagger têm `iss = http://localhost:8180/realms/tarefas`.
  Dentro do compose a aplicação alcança o Keycloak por `http://keycloak:8080`. Solução:
  `spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/tarefas`
  **e** `jwk-set-uri=http://keycloak:8080/realms/tarefas/protocol/openid-connect/certs`
  (Spring usa `jwk-set-uri` para as chaves e `issuer-uri` apenas para validar o claim).
- `compose.yaml`: serviços `keycloak` (healthcheck em `/realms/tarefas`), `app`
  (`depends_on: condition: service_healthy`), volume `dados-h2:/app/dados`.
- `Dockerfile` multi-stage: `maven:3.9-eclipse-temurin-21` (build, `-DskipTests`) →
  `eclipse-temurin:21-jre-alpine`, usuário não-root `app`, `EXPOSE 8080`.
**Alternativas**: `KC_HOSTNAME` fixo — funciona, mas a dupla configuração
`issuer-uri`/`jwk-set-uri` é mais explícita e não depende de flags do Keycloak.

## R-15. Estrutura de pacotes e nomenclatura em português

**Decisão** (Princípios I e II):

```
com.task.task_backend
├── config/        SegurancaConfig, OpenApiConfig, RelogioConfig
├── controller/    TarefaController
├── dto/           TarefaRequisicaoDTO, TarefaRespostaDTO, MoverTarefaRequisicaoDTO,
│                  RegistroHistoricoRespostaDTO, ResumoFinanceiroRespostaDTO
├── entity/        Tarefa, RegistroHistorico, Prioridade, Situacao, TipoRegistro,
│                  DestinoMovimento, FiltroSituacao
├── exception/     TratadorGlobalExcecoes, TarefaNaoEncontradaException,
│                  PosicaoInvalidaException, RespostaErroSeguranca
├── mapper/        TarefaMapper, RegistroHistoricoMapper
├── repository/    TarefaRepository, RegistroHistoricoRepository
├── seguranca/     UsuarioAutenticado, UsuarioLogado, ConversorAutoridadesKeycloak
├── service/       TarefaService, HistoricoService, ResumoFinanceiroService
└── util/          NormalizadorTexto, FormatadorHistorico
```

Testes espelham os pacotes em `unit/` e `integration/`, com sufixos `Test` e `IT`.

## R-16. Observabilidade e falha do IdP (itens "Deferred" da clarificação)

**Decisão**: Actuator expõe apenas `health` (público) e `info`; logs via `@Slf4j` em
nível INFO para operações de negócio (criar/editar/excluir/mover) com `id` da tarefa e
`sub` do usuário, nunca dados pessoais além do id. Se o JWKS do Keycloak estiver
indisponível na primeira validação, o Resource Server responde 401 com `ProblemDetail`
"Não foi possível validar o token de acesso"; chaves já obtidas ficam em cache pelo
`NimbusJwtDecoder`, então indisponibilidade transitória após o *warm-up* não afeta
requisições.
