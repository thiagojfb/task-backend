---

description: "Lista de tarefas para implementação da API de Gerenciamento de Tarefas"
---

# Tasks: API de Gerenciamento de Tarefas

**Input**: Design documents from `/specs/001-gerenciamento-tarefas-api/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Tests**: OBRIGATÓRIOS (Constituição, Princípio III — TDD com cobertura JaCoCo 100% em linhas e ramos). Em cada história, as tarefas de teste vêm ANTES das de implementação, devem ser escritas primeiro e FALHAR antes do código de produção. `./mvnw verify` deve passar ao final de cada fase.

**Organization**: Tarefas agrupadas por user story (US1–US6 da spec) para permitir implementação e teste independentes de cada história.

**Idioma**: todo código, nomes de classes/métodos/testes, mensagens, Javadoc, SQL, YAML e anotações OpenAPI em português do Brasil (Princípio II).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência de tarefa incompleta)
- **[Story]**: User story a que a tarefa pertence (US1..US6)
- Cada descrição inclui o caminho exato do arquivo

## Path Conventions

- Raiz de produção: `src/main/java/com/task/task_backend/` (abreviado abaixo como `PROD/`)
- Recursos de produção: `src/main/resources/`
- Raiz de testes: `src/test/java/com/task/task_backend/` (abreviado como `TEST/`)
- Recursos de teste: `src/test/resources/`
- Sufixos: testes de unidade `*Test.java`, testes de integração `*IT.java`
- Referências: **R-xx** = decisão em `research.md`; **FR-xxx** = requisito em `spec.md`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Projeto Maven completo, perfis, Keycloak e containers prontos para receber código.

- [x] T001 Reescrever `pom.xml` conforme R-01..R-04: dependências `spring-boot-starter-webmvc`, `spring-boot-starter-validation`, `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-actuator`, `com.h2database:h2` (runtime), `spring-boot-h2console` (runtime), `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1`, `org.projectlombok:lombok` (provided), `org.mapstruct:mapstruct:1.6.3`; testes: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, `spring-boot-starter-security-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-testcontainers`, `org.testcontainers:junit-jupiter`, `com.github.dasniko:testcontainers-keycloak:4.3.1`. Plugins: `maven-compiler-plugin` com `annotationProcessorPaths` na ordem lombok → `lombok-mapstruct-binding:0.2.0` → `mapstruct-processor:1.6.3`; `maven-surefire-plugin` com `<includes>**/*Test.java</includes><includes>**/*IT.java</includes>`; `jacoco-maven-plugin:0.8.15` com `prepare-agent`, `report` e `check` (fase `verify`, regras `LINE` e `BRANCH` mínimo `1.00`) e exclusões comentadas `com/task/task_backend/TaskBackendApplication.class` (bootstrap) e `com/task/task_backend/mapper/*Impl.class` (código gerado); `maven-resources-plugin` execução extra copiando `keycloak/realm-export.json` para `target/test-classes/keycloak/` na fase `generate-test-resources`. Preencher `<name>task-backend</name>` e `<description>` em português.
- [x] T002 [P] Criar `lombok.config` na raiz com `config.stopBubbling = true` e `lombok.addLombokGeneratedAnnotation = true` (JaCoCo ignora código gerado pelo Lombok, R-04)
- [x] T003 [P] Substituir `src/main/resources/application.properties` por `src/main/resources/application.yml` (nome `task-backend`, `spring.profiles.default: dev`, `spring.jpa.hibernate.ddl-auto: validate`, `spring.jpa.open-in-view: false`, `spring.flyway.enabled: true`, `springdoc.swagger-ui.path: /swagger-ui.html`, `management.endpoints.web.exposure.include: health,info`, `spring.jackson.time-zone: America/Sao_Paulo`), `src/main/resources/application-dev.yml` (`spring.datasource.url: jdbc:h2:file:./dados/tarefas`, usuário `sa`, `spring.h2.console.enabled: true`, `spring.security.oauth2.resourceserver.jwt.issuer-uri: http://localhost:8180/realms/tarefas`), `src/main/resources/application-docker.yml` (`jdbc:h2:file:/app/dados/tarefas`, `issuer-uri: ${KEYCLOAK_ISSUER_URI}`, `jwk-set-uri: ${KEYCLOAK_JWK_SET_URI}`, console H2 desabilitado) e `src/test/resources/application-test.yml` (`jdbc:h2:mem:tarefas;DB_CLOSE_DELAY=-1`, `jwk-set-uri: http://localhost:0/certs` como valor provisório sobrescrito pelos ITs via `@DynamicPropertySource`, R-07)
- [x] T004 [P] Adicionar `dados/` ao `.gitignore` (diretório do H2 em arquivo, R-07)
- [x] T005 [P] Criar `keycloak/realm-export.json` (R-14): realm `tarefas`; role de realm `usuario` atribuída explicitamente a cada usuário (não via default-roles, para que `visitante` fique sem ela); cliente público `tarefas-frontend` (standard flow + PKCE, redirect `http://localhost:*`); cliente público `tarefas-teste` com `directAccessGrantsEnabled: true`; usuários `ana` (nome "Ana Silva") e `bruno` (nome "Bruno Souza") com senha `senha123` e role `usuario`; usuário `visitante` com senha `senha123` SEM a role `usuario` e sem `default-roles` (para testar 403); usuário `semnome` com senha `senha123`, role `usuario` e sem `firstName`/`lastName` (edge case de nome ausente); token lifespan 300 s
- [x] T006 [P] Criar `Dockerfile` multi-stage (R-14): estágio `build` em `maven:3.9-eclipse-temurin-21` executando `mvn -B -DskipTests package`; estágio final `eclipse-temurin:21-jre-alpine`, criar usuário/grupo `app`, `WORKDIR /app`, copiar `target/*.jar` como `app.jar`, `RUN mkdir -p /app/dados && chown app:app /app/dados`, `USER app`, `EXPOSE 8080`, `ENV SPRING_PROFILES_ACTIVE=docker`, `ENTRYPOINT ["java","-jar","/app/app.jar"]`
- [x] T007 [P] Criar `compose.yaml` (R-14): serviço `keycloak` (`quay.io/keycloak/keycloak:26.7.4`, comando `start-dev --import-realm`, env `KC_BOOTSTRAP_ADMIN_USERNAME=admin`, `KC_BOOTSTRAP_ADMIN_PASSWORD=admin`, `KC_HEALTH_ENABLED=true`, volume `./keycloak:/opt/keycloak/data/import:ro`, porta `8180:8080`, healthcheck `curl -f http://localhost:8080/realms/tarefas || exit 1` via `bash -c 'exec 3<>/dev/tcp/localhost/9000 ...'` ou imagem com curl — escolher a forma que funcione na imagem oficial); serviço `app` (build `.`, porta `8080:8080`, env `KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/tarefas`, `KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/tarefas/protocol/openid-connect/certs`, `depends_on: keycloak: condition: service_healthy`, volume `dados-h2:/app/dados`); volume nomeado `dados-h2`
- [x] T008 Ajustar `src/test/java/com/task/task_backend/TaskBackendApplicationTests.java` para `@SpringBootTest` + `@ActiveProfiles("test")` mantendo `contextLoads()`; executar `./mvnw -q compile test-compile` e confirmar que compila com as novas dependências (o `verify` completo só passará após a Fase 2)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Esquema, entidades, utilitários, segurança, tratamento de erros, OpenAPI e infraestrutura de teste que TODAS as histórias usam.

**⚠️ CRITICAL**: Nenhuma user story pode começar antes desta fase estar completa.

- [x] T009 Criar `src/main/resources/db/migration/V1__criar_tabelas_tarefa_e_historico.sql` exatamente como em `data-model.md` (tabelas `tarefa` e `registro_historico`, checks, FK `ON DELETE CASCADE`, índices `idx_tarefa_usuario_posicao`, `idx_tarefa_usuario_normalizada`, `idx_historico_tarefa_data`)
- [x] T010 [P] Teste `TEST/unit/entity/EnumeracoesTest.java`: `Prioridade.getRotulo()` → Alta/Média/Baixa; `Situacao.getRotulo()` → Aberta/Concluída; `FiltroSituacao.paraSituacao()` → `TODAS`→`Optional.empty()`, `ABERTAS`→`ABERTA`, `CONCLUIDAS`→`CONCLUIDA`; `TipoRegistro` e `DestinoMovimento` possuem exatamente os valores do contrato
- [x] T011 [P] Criar enums em `PROD/entity/`: `Prioridade.java` (ALTA, MEDIA, BAIXA + `rotulo`), `Situacao.java` (ABERTA, CONCLUIDA + `rotulo`), `TipoRegistro.java` (CRIACAO, ALTERACAO, CONCLUSAO, REABERTURA), `DestinoMovimento.java` (TOPO, FINAL, POSICAO), `FiltroSituacao.java` (TODAS, ABERTAS, CONCLUIDAS + `paraSituacao()`)
- [x] T012 [P] Teste `TEST/unit/util/NormalizadorTextoTest.java`: `"Comprar Lâmpada"`→`"comprar lampada"`, `"Ação à Ç"`→`"acao a c"`, `null`→`""`, `"  Espaços  "`→`"espacos"` (trim), string vazia→`""`
- [x] T013 [P] Criar `PROD/util/NormalizadorTexto.java` (classe final com construtor privado, método estático `normalizar(String)` usando `Normalizer.Form.NFD`, remoção de `\p{M}`, `trim`, `toLowerCase(Locale.ROOT)`, R-09)
- [x] T014 [P] Teste `TEST/unit/util/FormatadorHistoricoTest.java`: `formatarValor(new BigDecimal("1250"))`→`"R$ 1.250,00"` (espaço comum U+0020, não NBSP); `formatarValor(new BigDecimal("50.5"))`→`"R$ 50,50"`; `formatarValor(null)`→`"R$ 0,00"`; `formatarData(LocalDate.of(2026,9,30))`→`"30/09/2026"`; `formatarData(null)`→`"—"`; `formatarDataHora(LocalDateTime)`→`"15/09/2026"` (apenas data para a frase); `formatarPrioridade(ALTA)`→`"Alta"`; `formatarSituacao(CONCLUIDA)`→`"Concluída"`
- [x] T015 [P] Criar `PROD/util/FormatadorHistorico.java` (métodos estáticos; `DecimalFormat("#,##0.00", DecimalFormatSymbols(new Locale("pt","BR")))` com prefixo literal `"R$ "`; `DateTimeFormatter.ofPattern("dd/MM/yyyy")`, R-10)
- [x] T016 [P] Teste `TEST/unit/entity/TarefaTest.java`: `@PrePersist` (`aoPersistir`) preenche `descricaoNormalizada` a partir de `descricao` e aplica padrões `situacao=ABERTA`, `valor=0.00` quando nulos; `@PreUpdate` (`aoAtualizar`) renormaliza após alterar `descricao`; `equals/hashCode` apenas por `id`; `RegistroHistorico` construído via builder mantém todos os campos
- [x] T017 [P] Criar entidades `PROD/entity/Tarefa.java` e `PROD/entity/RegistroHistorico.java` conforme `data-model.md` (Lombok `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode(onlyExplicitlyIncluded = true)`, SEM `@Data`; `@Enumerated(EnumType.STRING)`; `@Column` com nomes das colunas; `@PrePersist/@PreUpdate` usando `NormalizadorTexto`; `@ManyToOne(fetch = LAZY)` em `RegistroHistorico.tarefa`)
- [x] T018 [P] Criar `PROD/config/RelogioConfig.java` com bean `Clock relogio()` → `Clock.system(ZoneId.of("America/Sao_Paulo"))` (R-11)
- [x] T019 [P] Testes `TEST/unit/seguranca/UsuarioAutenticadoTest.java` (JWT com `name` → usa `name`; sem `name` mas com `preferred_username` → usa-o; só `sub` → usa `sub`; sem autenticação no contexto → lança `IllegalStateException` com mensagem em português) e `TEST/unit/seguranca/ConversorAutoridadesKeycloakTest.java` (`realm_access.roles=["usuario","outra"]` → `ROLE_usuario`, `ROLE_outra`; claim ausente → sem autoridades; `realm_access` sem `roles` → sem autoridades)
- [x] T020 [P] Criar `PROD/seguranca/UsuarioLogado.java` (`record(String id, String nome)`), `PROD/seguranca/UsuarioAutenticado.java` (`@Component`, método `obter()` lendo `Jwt` de `SecurityContextHolder`, R-06) e `PROD/seguranca/ConversorAutoridadesKeycloak.java` (`implements Converter<Jwt, Collection<GrantedAuthority>>`, extrai `realm_access.roles` com prefixo `ROLE_`)
- [x] T021 Criar exceções `PROD/exception/TarefaNaoEncontradaException.java` (mensagem "Não existe tarefa com o identificador informado.") e `PROD/exception/PosicaoInvalidaException.java` (construtor `(int minimo, int maximo)` → "A posição deve estar entre %d e %d."), e DTO `PROD/dto/ErroValidacaoDTO.java` (`record(String campo, String mensagem)`)
- [x] T022 [P] Teste `TEST/unit/exception/TratadorGlobalExcecoesTest.java` (instanciar diretamente e chamar handlers): `TarefaNaoEncontradaException` → `ProblemDetail` 404 título "Tarefa não encontrada"; `PosicaoInvalidaException` → 400 título "Posição inválida" com `detail` da exceção; `MethodArgumentNotValidException` → 400 título "Dados inválidos", propriedade `erros` com `campo`/`mensagem` ordenados por campo; `HttpMessageNotReadableException` (enum inválido) → 400 título "Corpo da requisição inválido"; `Exception` genérica → 500 título "Erro interno" sem vazar mensagem
- [x] T023 Criar `PROD/exception/TratadorGlobalExcecoes.java` (`@RestControllerAdvice extends ResponseEntityExceptionHandler`, sobrescrevendo `handleMethodArgumentNotValid` e `handleHttpMessageNotReadable`, mais `@ExceptionHandler` para as duas exceções de domínio e `Exception`; `@Slf4j` logando erros 500, R-12)
- [x] T024 Criar `PROD/config/SegurancaConfig.java`: `SecurityFilterChain` stateless, CSRF desabilitado, `permitAll` para `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`; `/api/**` → `hasRole("usuario")`; `anyRequest().denyAll()`; `oauth2ResourceServer().jwt()` com `JwtAuthenticationConverter` usando `ConversorAutoridadesKeycloak`; `AuthenticationEntryPoint` e `AccessDeniedHandler` que escrevem `ProblemDetail` JSON (`application/problem+json`) com títulos "Não autenticado" (401, detail "É necessário um token de acesso válido.") e "Acesso negado" (403, detail "Você não tem permissão para acessar este recurso.") via `ObjectMapper` injetado (R-06, R-12)
- [x] T025 [P] Criar `PROD/config/OpenApiConfig.java` com bean `OpenAPI` (título "API de Gerenciamento de Tarefas", versão 1.0.0, descrição em português, `SecurityScheme bearerAuth` HTTP bearer JWT, `SecurityRequirement` global) e tags "Tarefas", "Histórico", "Financeiro" (R-02)
- [x] T026 [P] Criar suporte de integração `TEST/integration/KeycloakContainerSuporte.java`: classe abstrata `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Testcontainers`; `static KeycloakContainer` (`quay.io/keycloak/keycloak:26.7.4`) com `withRealmImportFile("/keycloak/realm-export.json")` iniciado uma vez (campo static + `start()` em bloco estático ou `@Container` static); `@DynamicPropertySource` definindo `spring.security.oauth2.resourceserver.jwt.issuer-uri` e `jwk-set-uri` a partir de `container.getAuthServerUrl() + "/realms/tarefas"`; método `protected String obterToken(String usuario, String senha)` fazendo password grant no cliente `tarefas-teste` com `RestClient`; constantes `ANA`, `BRUNO`, `VISITANTE`, `SEMNOME`, `SENHA`; método `protected String bearer(String usuario)` retornando `"Bearer " + token` (R-05)
- [x] T027 Testes de segurança e documentação: `TEST/integration/SegurancaIT.java` (GET `/api/v1/tarefas` sem token → 401 `application/problem+json` título "Não autenticado"; token de `visitante` → 403 título "Acesso negado"; token inválido → 401; `/actuator/health` sem token → 200) e `TEST/integration/OpenApiIT.java` (`/v3/api-docs` sem token → 200 com `info.title` = "API de Gerenciamento de Tarefas" e `components.securitySchemes.bearerAuth`; `/swagger-ui/index.html` → 200) — ambos estendendo `KeycloakContainerSuporte`; nesta fase esperam-se os 401/403/200 mesmo sem endpoints de tarefas (o `/api/v1/tarefas` inexistente ainda responde 401/403 antes do 404)
- [x] T028 Teste `TEST/integration/TarefaRepositoryIT.java` (`@DataJpaTest @ActiveProfiles("test")` com Flyway habilitado e `@AutoConfigureTestDatabase(replace = NONE)`): `findByUsuarioIdOrderByPosicaoAsc` retorna em ordem de posição apenas do usuário; `findByIdAndUsuarioId` vazio para outro usuário; `countByUsuarioId`; `RegistroHistoricoRepository.findByTarefaIdOrderByDataHoraDesc` retorna mais recente primeiro
- [x] T029 Criar `PROD/repository/TarefaRepository.java` (`extends JpaRepository<Tarefa, UUID>` com `findByUsuarioIdOrderByPosicaoAsc`, `findByIdAndUsuarioId`, `countByUsuarioId`) e `PROD/repository/RegistroHistoricoRepository.java` (`findByTarefaIdOrderByDataHoraDesc`); executar `./mvnw verify` — deve ficar verde (cobertura 100% do que existe até aqui)

**Checkpoint**: Fundação pronta — contexto sobe com perfil `test`, Keycloak efêmero autentica, erros saem como `ProblemDetail` em português, Swagger acessível.

---

## Phase 3: User Story 1 - Cadastrar e visualizar tarefas (Priority: P1) 🎯 MVP

**Goal**: Usuário autenticado cria tarefas (data de abertura automática, situação ABERTA, última posição, valor padrão 0,00) e lista/consulta apenas as suas.

**Independent Test**: cadastrar três tarefas com dados distintos via `POST /api/v1/tarefas`, listar via `GET` e ver posições 1..3 com todos os campos; `bruno` não enxerga tarefas de `ana`; sem token → 401.

### Tests for User Story 1 (MANDATORY — Princípio III) ⚠️

> **NOTE: escrever estes testes PRIMEIRO e garantir que FALHEM antes da implementação**

- [x] T030 [P] [US1] Teste `TEST/unit/mapper/TarefaMapperTest.java` (instanciar `Mappers.getMapper` ou `@SpringBootTest` slice mínimo): `paraResposta(tarefa, hoje)` copia todos os campos e calcula `atrasada` = true quando ABERTA e `dataPrevistaConclusao < hoje`; false quando CONCLUIDA mesmo com prazo passado; false quando prazo nulo; false quando prazo hoje; `paraEntidade(requisicao)` ignora `id`, `posicao`, `dataAbertura`, `situacao`, `usuarioId` e converte `valor` nulo em `0.00` com escala 2
- [x] T031 [P] [US1] Teste `TEST/unit/service/TarefaServiceTest.java` (Mockito, `Clock.fixed`): `criar` define `usuarioId` do `UsuarioAutenticado`, `dataAbertura = LocalDateTime.now(clock)`, `situacao = ABERTA`, `posicao = countByUsuarioId + 1`, `valor` com escala 2, chama `historicoService.registrarCriacao(tarefa, usuario)` e retorna DTO; `listar(TODAS, null)` delega a `findByUsuarioIdOrderByPosicaoAsc` e mapeia; `obter(id)` retorna DTO ou lança `TarefaNaoEncontradaException` quando `findByIdAndUsuarioId` vazio
- [x] T032 [P] [US1] Teste `TEST/unit/service/HistoricoServiceTest.java`: `registrarCriacao` salva `RegistroHistorico` com `tipo=CRIACAO`, `campo/valorAnterior/valorNovo` nulos, `usuarioId/usuarioNome` do `UsuarioLogado`, `dataHora = now(clock)`
- [x] T033 [P] [US1] Teste `TEST/unit/controller/TarefaControllerTest.java` (`@WebMvcTest(TarefaController.class)` importando `SegurancaConfig`, `TratadorGlobalExcecoes`, `ConversorAutoridadesKeycloak`; `@MockitoBean` para serviços; requisições com `.with(jwt().jwt(j -> j.subject("ana-id").claim("name","Ana Silva")).authorities(new SimpleGrantedAuthority("ROLE_usuario")))`): POST 201 com header `Location` e corpo; POST 400 para descrição em branco (mensagem "A descrição é obrigatória."), descrição > 500 chars ("A descrição deve ter no máximo 500 caracteres."), prioridade ausente ("A prioridade é obrigatória."), prioridade inválida no JSON (400 "Corpo da requisição inválido"), valor negativo ("O valor não pode ser negativo."), valor com 3 decimais ("O valor deve ter no máximo duas casas decimais."); GET lista 200 com array; GET `/{id}` 200 e 404 `application/problem+json`; GET sem jwt → 401; GET com jwt sem `ROLE_usuario` → 403
- [x] T034 [P] [US1] Teste `TEST/integration/TarefaCrudIT.java` (estende `KeycloakContainerSuporte`, `@Transactional` ou limpeza via repositório em `@AfterEach`) cobrindo os 6 cenários da US1: criação completa com data de abertura ≈ agora, situação ABERTA, posição 1; terceira tarefa recebe posição 3 sem alterar as anteriores; valor ausente → `0.00`; POST inválido → 400 com `erros[]` em português; sem token → 401; `bruno` lista vazio enquanto `ana` tem tarefas, e `GET /{id}` de tarefa de `ana` com token de `bruno` → 404

### Implementation for User Story 1

- [x] T035 [P] [US1] Criar DTOs `PROD/dto/TarefaRequisicaoDTO.java` (`record` com `@NotBlank(message="A descrição é obrigatória.") @Size(max=500, message="A descrição deve ter no máximo 500 caracteres.") String descricao`, `@NotNull(message="A prioridade é obrigatória.") Prioridade prioridade`, `LocalDate dataPrevistaConclusao`, `@DecimalMin(value="0.00", message="O valor não pode ser negativo.") @Digits(integer=13, fraction=2, message="O valor deve ter no máximo duas casas decimais.") BigDecimal valor`; `@Schema` em português em cada campo) e `PROD/dto/TarefaRespostaDTO.java` (`record` com os 10 campos do contrato `TarefaResposta`, `@Schema`)
- [x] T036 [US1] Criar `PROD/mapper/TarefaMapper.java` (`@Mapper(componentModel="spring")`; `paraEntidade(TarefaRequisicaoDTO)` com `@Mapping(target=..., ignore=true)` para campos não editáveis e método `default BigDecimal normalizarValor(BigDecimal)`; `paraResposta(Tarefa tarefa, @Context LocalDate hoje)` com método `default boolean calcularAtrasada(...)`)
- [x] T037 [US1] Criar `PROD/service/HistoricoService.java` (`@Service @RequiredArgsConstructor @Slf4j`; depende de `RegistroHistoricoRepository` e `Clock`; método `registrarCriacao(Tarefa, UsuarioLogado)`)
- [x] T038 [US1] Criar `PROD/service/TarefaService.java` (`@Service @RequiredArgsConstructor @Slf4j`; depende de `TarefaRepository`, `TarefaMapper`, `HistoricoService`, `UsuarioAutenticado`, `Clock`; métodos `@Transactional criar(TarefaRequisicaoDTO)`, `@Transactional(readOnly=true) listar(FiltroSituacao, String)` (nesta fase só TODAS/sem busca), `@Transactional(readOnly=true) obter(UUID)`; método privado `buscarDoUsuario(UUID)` lançando `TarefaNaoEncontradaException`; logs INFO "Tarefa {} criada pelo usuário {}")
- [x] T039 [US1] Criar `PROD/controller/TarefaController.java` (`@RestController @RequestMapping("/api/v1/tarefas") @RequiredArgsConstructor @Tag(name="Tarefas")`; `POST` → 201 + `Location` via `ServletUriComponentsBuilder`; `GET` lista com `@RequestParam(defaultValue="TODAS") FiltroSituacao situacao` e `@RequestParam(required=false) String busca`; `GET /{id}`; cada método com `@Operation(summary=..., description=...)` e `@ApiResponses` em português conforme `contracts/openapi.yaml`)
- [x] T040 [US1] Executar `./mvnw verify`: todos os testes verdes e `jacoco:check` a 100%; corrigir ramos descobertos antes de avançar

**Checkpoint**: US1 funcional e testável isoladamente — MVP entregável.

---

## Phase 4: User Story 2 - Editar e concluir tarefas (Priority: P1)

**Goal**: Alterar descrição/prioridade/prazo/valor de qualquer tarefa própria (aberta ou concluída), concluir registrando data real e reabrir apagando-a; histórico por campo alterado.

**Independent Test**: criar tarefa, `PUT` alterando valor e prioridade, `PATCH /concluir`, `PATCH /reabrir`; consulta reflete mudanças e situação; edição de tarefa alheia → 404.

### Tests for User Story 2 (MANDATORY — Princípio III) ⚠️

- [x] T041 [P] [US2] Ampliar `TEST/unit/service/HistoricoServiceTest.java`: `registrarAlteracoes(anterior, atual, usuario)` gera um registro `ALTERACAO` por campo diferente (descricao, prioridade, dataPrevistaConclusao, valor) com valores formatados por `FormatadorHistorico` (ex.: `"R$ 50,00"`→`"R$ 70,00"`, prazo nulo → `"—"`); nenhum registro quando todos iguais (inclusive `50.0` vs `50.00`); `registrarMudancaSituacao(tarefa, ABERTA, CONCLUIDA, usuario)` gera `CONCLUSAO` com `campo="situacao"`, `"Aberta"`→`"Concluída"`; inverso gera `REABERTURA`
- [x] T042 [P] [US2] Ampliar `TEST/unit/service/TarefaServiceTest.java`: `editar(id, dto)` aplica os 4 campos, mantém `dataAbertura/posicao/situacao/usuarioId`, chama `registrarAlteracoes` com cópia do estado anterior, funciona em tarefa CONCLUIDA; `concluir(id)` em ABERTA define `situacao=CONCLUIDA`, `dataConclusaoReal=now(clock)` e chama `registrarMudancaSituacao`; `concluir` em já CONCLUIDA não altera nada nem registra histórico; `reabrir(id)` em CONCLUIDA volta a ABERTA, `dataConclusaoReal=null`, registra `REABERTURA`; `reabrir` em já ABERTA é no-op; todos lançam `TarefaNaoEncontradaException` para id alheio
- [x] T043 [P] [US2] Ampliar `TEST/unit/controller/TarefaControllerTest.java`: `PUT /{id}` 200 com corpo, 400 validação, 404; `PATCH /{id}/concluir` 200 e 404; `PATCH /{id}/reabrir` 200 e 404
- [x] T044 [P] [US2] Ampliar `TEST/integration/TarefaCrudIT.java` com os 5 cenários da US2: alterar valor 50→70 mantém demais campos; concluir → CONCLUIDA com `dataConclusaoReal` preenchida; reabrir → ABERTA e `dataConclusaoReal` nula; PUT em tarefa de `ana` com token de `bruno` → 404; PUT em UUID inexistente → 404

### Implementation for User Story 2

- [x] T045 [US2] Implementar em `PROD/service/HistoricoService.java` os métodos `registrarAlteracoes(Tarefa anterior, Tarefa atual, UsuarioLogado)` (comparação com `Objects.equals` e `BigDecimal.compareTo` para valor; nomes de campo `descricao`, `prioridade`, `dataPrevistaConclusao`, `valor`) e `registrarMudancaSituacao(Tarefa, Situacao de, Situacao para, UsuarioLogado)`
- [x] T046 [US2] Implementar em `PROD/service/TarefaService.java` os métodos `@Transactional editar(UUID, TarefaRequisicaoDTO)` (snapshot via `toBuilder`/cópia antes de aplicar), `@Transactional concluir(UUID)` e `@Transactional reabrir(UUID)` (idempotentes)
- [x] T047 [US2] Adicionar em `PROD/controller/TarefaController.java` os endpoints `PUT /{id}`, `PATCH /{id}/concluir`, `PATCH /{id}/reabrir` com anotações OpenAPI em português; executar `./mvnw verify` (100%)

**Checkpoint**: US1 e US2 funcionam de forma independente; histórico já é gravado (consulta chega na US4).

---

## Phase 5: User Story 3 - Reordenar tarefas na lista (Priority: P2)

**Goal**: Mover tarefa para TOPO, FINAL ou POSICAO específica mantendo posições contínuas 1..N; posição fora do intervalo → 400; mover para a própria posição é no-op; não gera histórico.

**Independent Test**: criar 5 tarefas; mover a 5ª ao topo, a 1ª ao final e a 4ª para a posição 2; verificar sequência resultante e ausência de lacunas.

### Tests for User Story 3 (MANDATORY — Princípio III) ⚠️

- [x] T048 [P] [US3] Ampliar `TEST/integration/TarefaRepositoryIT.java`: `incrementarPosicoes(usuario, 2, 4)` soma 1 apenas nas posições 2..4 do usuário (outros usuários intactos); `decrementarPosicoes(usuario, 2, 4)` subtrai 1; `buscarParaMover(id, usuario)` retorna a tarefa (lock pessimista dentro de `@Transactional`) e vazio para outro usuário
- [x] T049 [P] [US3] Ampliar `TEST/unit/service/TarefaServiceTest.java`: `mover(id, TOPO)` com posição 5 de 5 → `incrementarPosicoes(u,1,4)` e `posicao=1`; `mover(id, FINAL)` com posição 1 de 5 → `decrementarPosicoes(u,2,5)` e `posicao=5`; `mover(id, POSICAO, 2)` com posição 4 → `incrementarPosicoes(u,2,3)`; `mover` para a posição atual → nenhuma chamada de shift; `POSICAO` com `posicao` nula, 0 ou `N+1` → `PosicaoInvalidaException(1, N)`; TOPO/FINAL ignoram `posicao`; nenhuma chamada a `HistoricoService`; id alheio → `TarefaNaoEncontradaException`
- [x] T050 [P] [US3] Ampliar `TEST/unit/controller/TarefaControllerTest.java`: `PATCH /{id}/mover` 200 com `{"destino":"TOPO"}`; 400 "Dados inválidos" com `destino` ausente ("O destino é obrigatório."); 400 "Posição inválida" quando serviço lança `PosicaoInvalidaException` (detail "A posição deve estar entre 1 e 5."); 400 "Corpo da requisição inválido" para destino desconhecido; 404
- [x] T051 [P] [US3] Criar `TEST/integration/TarefaReordenacaoIT.java` (estende `KeycloakContainerSuporte`) com os 5 cenários da US3 (5→topo, 1→final, 4→posição 2 resultando `[1,4,2,3,5]`, posição 9 e 0 → 400 com mensagem do intervalo, sequência sempre contínua) e um teste de propriedade: 30 movimentos aleatórios com `Random(42)` entre TOPO/FINAL/POSICAO válidos → ao final `posicoes == [1..N]` e `bruno` mantém sua própria sequência intacta

### Implementation for User Story 3

- [x] T052 [US3] Criar `PROD/dto/MoverTarefaRequisicaoDTO.java` (`record` com `@NotNull(message="O destino é obrigatório.") DestinoMovimento destino`, `@Positive(message="A posição deve ser maior que zero.") Integer posicao`, `@Schema`)
- [x] T053 [US3] Adicionar em `PROD/repository/TarefaRepository.java`: `@Lock(LockModeType.PESSIMISTIC_WRITE) @Query Optional<Tarefa> buscarParaMover(UUID id, String usuarioId)`, `@Modifying @Query int incrementarPosicoes(String usuarioId, int inicio, int fim)`, `@Modifying @Query int decrementarPosicoes(String usuarioId, int inicio, int fim)` (JPQL com `BETWEEN`, `clearAutomatically = true`, R-08)
- [x] T054 [US3] Implementar `@Transactional mover(UUID, MoverTarefaRequisicaoDTO)` em `PROD/service/TarefaService.java` (resolver destino → posição alvo via `switch` com padrões; validar 1..N; shifts conforme R-08; log INFO)
- [x] T055 [US3] Adicionar `PATCH /{id}/mover` em `PROD/controller/TarefaController.java` com OpenAPI em português; executar `./mvnw verify` (100%)

**Checkpoint**: Reordenação completa e provada por propriedade.

---

## Phase 6: User Story 4 - Consultar histórico de alterações (Priority: P2)

**Goal**: Expor a linha do tempo da tarefa (mais recente primeiro) com autor, data/hora, campo, valores formatados e frase pronta para a interface.

**Independent Test**: criar tarefa, alterar valor e prioridade em edições distintas, concluir; `GET /{id}/historico` retorna CONCLUSAO, ALTERACAO(prioridade), ALTERACAO(valor), CRIACAO nessa ordem, com frases em português.

### Tests for User Story 4 (MANDATORY — Princípio III) ⚠️

- [x] T056 [P] [US4] Teste `TEST/unit/mapper/RegistroHistoricoMapperTest.java`: `paraResposta` monta `descricao` = `"Criada por Ana Silva em 15/09/2026"` para CRIACAO; `"Modificado por Ana Silva em 15/09/2026 - Alterou valor de R$ 50,00 para R$ 70,00"` para ALTERACAO (rótulo do campo em português: valor, descrição, prioridade, data prevista de conclusão); `"Concluída por Ana Silva em 15/09/2026"` para CONCLUSAO; `"Reaberta por Ana Silva em 15/09/2026"` para REABERTURA; copia demais campos
- [x] T057 [P] [US4] Ampliar `TEST/unit/service/TarefaServiceTest.java`: `listarHistorico(id)` valida propriedade da tarefa (404 para alheia) e delega a `findByTarefaIdOrderByDataHoraDesc` mapeando para DTO
- [x] T058 [P] [US4] Ampliar `TEST/unit/controller/TarefaControllerTest.java`: `GET /{id}/historico` 200 com array e 404
- [x] T059 [P] [US4] Criar `TEST/integration/TarefaHistoricoIT.java` (estende `KeycloakContainerSuporte`) com os 6 cenários da US4: alteração 50→70 gera registro com `usuarioNome="Ana Silva"`, `campo="valor"`, `"R$ 50,00"`, `"R$ 70,00"`; edição de descrição+prioridade gera 2 registros; PUT idêntico gera 0 novos; concluir gera `CONCLUSAO` `Aberta→Concluída`; tarefa nova tem exatamente 1 registro CRIACAO; mover não gera registro; extra: usuário `semnome` tem `usuarioNome` igual ao `preferred_username` ou `sub`

### Implementation for User Story 4

- [x] T060 [US4] Criar `PROD/dto/RegistroHistoricoRespostaDTO.java` (`record` com `id, usuarioNome, dataHora, tipo, campo, valorAnterior, valorNovo, descricao`, `@Schema` com exemplos do contrato)
- [x] T061 [US4] Criar `PROD/mapper/RegistroHistoricoMapper.java` (`@Mapper(componentModel="spring")` com método `default String montarDescricao(RegistroHistorico)` usando `switch` sobre `TipoRegistro` e `FormatadorHistorico.formatarDataHora`; mapa de rótulos de campo em português)
- [x] T062 [US4] Implementar `@Transactional(readOnly=true) listarHistorico(UUID)` em `PROD/service/TarefaService.java` (injetar `RegistroHistoricoRepository` e `RegistroHistoricoMapper`)
- [x] T063 [US4] Adicionar `GET /{id}/historico` em `PROD/controller/TarefaController.java` (`@Tag(name="Histórico")` no método) com OpenAPI em português; executar `./mvnw verify` (100%)

**Checkpoint**: Histórico consultável com frases prontas para o front.

---

## Phase 7: User Story 5 - Excluir tarefa (Priority: P3)

**Goal**: Exclusão definitiva da tarefa própria, removendo histórico em cascata e fechando a lacuna de posições.

**Independent Test**: criar 3 tarefas, `DELETE` a da posição 2; lista fica com posições 1 e 2; `GET /{id}/historico` da excluída → 404.

### Tests for User Story 5 (MANDATORY — Princípio III) ⚠️

- [x] T064 [P] [US5] Ampliar `TEST/integration/TarefaRepositoryIT.java`: `decrementarPosicoesApos(usuario, 2)` subtrai 1 apenas das posições > 2; `deleteById` de tarefa com registros de histórico remove os registros (cascata do banco) — verificar `RegistroHistoricoRepository.findByTarefaIdOrderByDataHoraDesc` vazio após `flush`
- [x] T065 [P] [US5] Ampliar `TEST/unit/service/TarefaServiceTest.java`: `excluir(id)` busca do usuário (404 alheia), chama `delete` e `decrementarPosicoesApos(u, posicaoExcluida)`; não chama `HistoricoService`
- [x] T066 [P] [US5] Ampliar `TEST/unit/controller/TarefaControllerTest.java`: `DELETE /{id}` 204 sem corpo e 404
- [x] T067 [P] [US5] Ampliar `TEST/integration/TarefaCrudIT.java` com os 3 cenários da US5: excluir posição 2 de 3 → restantes 1 e 2; histórico/edição da excluída → 404; DELETE de tarefa de `ana` com token de `bruno` → 404 e tarefa continua existindo para `ana`

### Implementation for User Story 5

- [x] T068 [US5] Adicionar `@Modifying @Query int decrementarPosicoesApos(String usuarioId, int posicao)` em `PROD/repository/TarefaRepository.java`
- [x] T069 [US5] Implementar `@Transactional excluir(UUID)` em `PROD/service/TarefaService.java` (log INFO "Tarefa {} excluída pelo usuário {}")
- [x] T070 [US5] Adicionar `DELETE /{id}` (`@ResponseStatus(NO_CONTENT)`) em `PROD/controller/TarefaController.java` com OpenAPI; executar `./mvnw verify` (100%)

**Checkpoint**: Ciclo de vida completo da tarefa (criar → editar → concluir → mover → excluir).

---

## Phase 8: User Story 6 - Filtrar, buscar e acompanhar gastos (Priority: P3)

**Goal**: Filtro por situação (TODAS/ABERTAS/CONCLUIDAS), busca por trecho da descrição sem acento/caixa, combináveis; indicador "total a gastar no mês" (tarefas abertas com prazo no mês corrente, fuso de Brasília).

**Independent Test**: com tarefas abertas/concluídas, com/sem valor, dentro/fora do mês: cada filtro retorna o subconjunto correto, `busca=lampada` encontra "Comprar lâmpada", resumo soma apenas abertas do mês.

### Tests for User Story 6 (MANDATORY — Princípio III) ⚠️

- [x] T071 [P] [US6] Ampliar `TEST/integration/TarefaRepositoryIT.java`: `buscar(usuario, null, "lu")` retorna "Pagar luz" e não "Comprar lâmpada"; `buscar(usuario, null, "lampada")` retorna "Comprar lâmpada"; `buscar(usuario, ABERTA, "")` retorna só abertas em ordem de posição; `somarValorAbertasNoPeriodo(usuario, 2026-09-01, 2026-09-30)` soma apenas abertas com prazo no intervalo e retorna `0` quando não há
- [x] T072 [P] [US6] Ampliar `TEST/unit/service/TarefaServiceTest.java`: `listar(ABERTAS, null)` usa `findByUsuarioIdAndSituacaoOrderByPosicaoAsc`; `listar(TODAS, " Lâmpada ")` normaliza o termo para `"lampada"` e chama `buscar(u, null, "lampada")`; `listar(CONCLUIDAS, "x")` chama `buscar(u, CONCLUIDA, "x")`; busca em branco equivale a nula
- [x] T073 [P] [US6] Teste `TEST/unit/service/ResumoFinanceiroServiceTest.java` (`Clock.fixed` em 2026-09-19 America/Sao_Paulo): `obter()` consulta `somarValorAbertasNoPeriodo(u, 2026-09-01, 2026-09-30)`, retorna `mes=9`, `ano=2026`, `totalAGastar=1250.00`, `totalFormatado="R$ 1.250,00"`; soma nula → `0.00` / `"R$ 0,00"`; mês de fevereiro em ano bissexto usa dia 29
- [x] T074 [P] [US6] Ampliar `TEST/unit/controller/TarefaControllerTest.java`: `GET ?situacao=ABERTAS&busca=lu` repassa parâmetros ao serviço; `situacao` inválida → 400; `GET /resumo-financeiro` 200 com os 4 campos
- [x] T075 [P] [US6] Criar `TEST/integration/TarefaFiltroBuscaFinanceiroIT.java` (estende `KeycloakContainerSuporte`) com os 5 cenários da US6: 3 abertas + 2 concluídas → filtro ABERTAS retorna 3 em ordem; busca "LU" e "lampada"; resumo = 1.250,00 com abertas de 1.000 e 250 no mês, concluída de 500 no mês e aberta de 300 no mês seguinte (datas relativas a `LocalDate.now(ZoneId.of("America/Sao_Paulo"))`); sem abertas no mês → `R$ 0,00`; filtro + busca combinados respeitam ambos

### Implementation for User Story 6

- [x] T076 [US6] Criar `PROD/dto/ResumoFinanceiroRespostaDTO.java` (`record(int mes, int ano, BigDecimal totalAGastar, String totalFormatado)`, `@Schema`)
- [x] T077 [US6] Adicionar em `PROD/repository/TarefaRepository.java`: `findByUsuarioIdAndSituacaoOrderByPosicaoAsc`, `@Query List<Tarefa> buscar(String usuarioId, Situacao situacao, String termoNormalizado)` (`(:situacao IS NULL OR t.situacao = :situacao) AND t.descricaoNormalizada LIKE CONCAT('%', :termo, '%') ORDER BY t.posicao`) e `@Query BigDecimal somarValorAbertasNoPeriodo(String usuarioId, LocalDate inicio, LocalDate fim)` (`COALESCE(SUM(t.valor), 0)`)
- [x] T078 [US6] Completar `listar(FiltroSituacao, String)` em `PROD/service/TarefaService.java` (normalizar termo com `NormalizadorTexto`; escolher finder por filtro/busca)
- [x] T079 [US6] Criar `PROD/service/ResumoFinanceiroService.java` (`@Service @RequiredArgsConstructor`; `Clock`, `TarefaRepository`, `UsuarioAutenticado`; `YearMonth.now(clock)` → `atDay(1)`/`atEndOfMonth()`; `FormatadorHistorico.formatarValor`)
- [x] T080 [US6] Adicionar `GET /resumo-financeiro` (`@Tag(name="Financeiro")`) em `PROD/controller/TarefaController.java` e documentar os parâmetros `situacao`/`busca` do `GET` lista com `@Parameter` em português; executar `./mvnw verify` (100%)

**Checkpoint**: Todas as 6 user stories independentes e funcionais.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Fechar cobertura, documentação, container e validação ponta a ponta.

- [x] T081 Executar `./mvnw verify` e abrir `target/site/jacoco/index.html`; cobrir todo ramo/linha restante (ex.: ramos de `switch`, `Optional`, handlers 401/403, `default` de enums) até `jacoco:check` passar sem violações; nenhuma nova exclusão sem justificativa comentada no `pom.xml`
- [x] T082 [P] Criar `README.md` em português na raiz: visão geral, stack, como subir (`docker compose up --build`), como obter token, link para Swagger, como rodar testes/cobertura, estrutura de pacotes, referência à constituição e à spec
- [x] T083 [P] Revisar Swagger: subir `docker compose up keycloak -d` + `./mvnw spring-boot:run`, abrir `http://localhost:8080/swagger-ui.html`, autorizar com token de `ana` e conferir que todos os 10 endpoints têm `summary`/`description`/respostas em português e que `bearerAuth` funciona; comparar `/v3/api-docs` com `specs/001-gerenciamento-tarefas-api/contracts/openapi.yaml` e corrigir divergências no código (o contrato de referência é a fonte)
- [x] T084 Endurecimento de segurança: revisar `PROD/config/SegurancaConfig.java` (deny-by-default confirmado, lista pública mínima, headers de segurança padrão do Spring Security), revisar logs em `PROD/service/*.java` para garantir que só registram `id` da tarefa e `sub` do usuário (nunca descrição/valor), e confirmar que `application-docker.yml` não possui segredos
- [x] T085 Validar containers: `docker compose up --build`, aguardar healthcheck, executar todos os comandos de `specs/001-gerenciamento-tarefas-api/quickstart.md` (seções 2 e 3) e confirmar as saídas esperadas; `docker compose down` e `up` novamente para comprovar persistência do H2 no volume `dados-h2`
- [x] T086 Rodada final: `./mvnw clean verify` verde com 100% e `git status` limpo de artefatos indevidos (`dados/`, `target/` ignorados); atualizar `specs/001-gerenciamento-tarefas-api/checklists/requirements.md` com nota de conclusão da implementação

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências — começar imediatamente
- **Foundational (Phase 2)**: depende da Fase 1 — BLOQUEIA todas as user stories
- **User Stories (Phase 3–8)**: todas dependem da Fase 2
  - Ordem recomendada (sequencial, por prioridade): US1 → US2 → US3 → US4 → US5 → US6
  - Vários arquivos são compartilhados entre histórias (`TarefaService`, `TarefaController`, `TarefaRepository`, `TarefaServiceTest`, `TarefaControllerTest`, `TarefaCrudIT`); por isso as histórias NÃO devem ser executadas em paralelo por agentes diferentes sem coordenação de merge
- **Polish (Phase 9)**: depende de todas as histórias desejadas concluídas

### User Story Dependencies

- **US1 (P1)**: apenas Fase 2 — base de todas as outras (cria `TarefaService`, `TarefaController`, `TarefaMapper`, `HistoricoService`)
- **US2 (P1)**: depende de US1 (edita as mesmas classes); histórico por campo é gravado aqui
- **US3 (P2)**: depende de US1 (precisa criar tarefas para mover); independente de US2
- **US4 (P2)**: depende de US1 e US2 (precisa de alterações registradas para consultar); leitura apenas
- **US5 (P3)**: depende de US1; usa cascata do banco (Fase 2) e reorganização similar à US3
- **US6 (P3)**: depende de US1 e US2 (precisa de tarefas concluídas para filtrar)

### Within Each User Story

- Testes DEVEM ser escritos e FALHAR antes da implementação (Princípio III)
- Migration → entity → DTO/mapper → repository → service → controller
- `./mvnw verify` verde com 100% ao final de cada história (última tarefa de cada fase)

### Parallel Opportunities

- Fase 1: T002–T007 em paralelo após T001 iniciar (arquivos distintos)
- Fase 2: pares teste/implementação T010–T020, T022, T025, T026 em paralelo; T021→T023→T024 sequenciais; T027 após T024–T026; T028→T029 no fim
- Em cada história: todas as tarefas de teste marcadas [P] podem ser escritas simultaneamente; DTOs [P] podem ser criados enquanto os testes são escritos
- Fase 9: T082 e T083 em paralelo

---

## Parallel Example: User Story 1

```bash
# Escrever todos os testes da US1 juntos (devem falhar — as classes ainda não existem):
Task: "TarefaMapperTest em src/test/java/com/task/task_backend/unit/mapper/TarefaMapperTest.java"
Task: "TarefaServiceTest em src/test/java/com/task/task_backend/unit/service/TarefaServiceTest.java"
Task: "HistoricoServiceTest em src/test/java/com/task/task_backend/unit/service/HistoricoServiceTest.java"
Task: "TarefaControllerTest em src/test/java/com/task/task_backend/unit/controller/TarefaControllerTest.java"
Task: "TarefaCrudIT em src/test/java/com/task/task_backend/integration/TarefaCrudIT.java"

# Em paralelo com os testes, criar os DTOs (não dependem de nada):
Task: "TarefaRequisicaoDTO e TarefaRespostaDTO em src/main/java/com/task/task_backend/dto/"

# Depois, sequencialmente: TarefaMapper → HistoricoService → TarefaService → TarefaController → ./mvnw verify
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Fase 1: Setup (T001–T008)
2. Completar Fase 2: Foundational (T009–T029) — CRÍTICO, bloqueia tudo
3. Completar Fase 3: US1 (T030–T040)
4. **PARAR e VALIDAR**: `./mvnw verify` 100%; `docker compose up --build`; criar e listar tarefas via Swagger com token de `ana`
5. Demonstrar o MVP

### Incremental Delivery

1. Setup + Foundational → fundação pronta (Keycloak + H2 + Swagger + erros em português)
2. US1 → cadastrar/listar → **MVP**
3. US2 → editar/concluir/reabrir
4. US3 → reordenar (topo/final/posição)
5. US4 → consultar histórico
6. US5 → excluir
7. US6 → filtros, busca e resumo financeiro
8. Polish → README, Swagger revisado, quickstart validado, containers persistentes

Cada incremento passa por `./mvnw verify` com cobertura 100% antes do commit (`feat:`/`test:` em português, Conventional Commits).

### Parallel Team Strategy

Com mais de um desenvolvedor/agente:

1. Todos concluem Setup + Foundational juntos
2. Após a Fase 2: US1 é pré-requisito de todas — fazê-la primeiro, em conjunto
3. Depois de US1: um agente segue US2 → US4 (fluxo de histórico) enquanto outro segue US3 → US5 (fluxo de posições); coordenar merges em `TarefaService`/`TarefaController`/testes compartilhados
4. US6 e Polish ao final, em conjunto

---

## Notes

- [P] = arquivos diferentes, sem dependência de tarefa incompleta
- [Story] mapeia a tarefa à user story da spec para rastreabilidade
- Cada user story é completável e testável de forma independente
- Verificar que os testes falham antes de implementar
- Commit após cada tarefa ou grupo lógico; mensagens em português
- Parar em qualquer checkpoint para validar a história isoladamente
- Docker precisa estar em execução para os testes `*IT` (Testcontainers Keycloak)
- Evitar: tarefas vagas, conflitos no mesmo arquivo em paralelo, dependências cruzadas que quebrem a independência
