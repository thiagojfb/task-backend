# Task Backend — API de Gerenciamento de Tarefas

API REST de uma lista de tarefas pessoais com prioridade, controle financeiro (valor em reais),
reordenação manual, histórico automático de alterações e autenticação via Keycloak.
Cada usuário enxerga e manipula somente as próprias tarefas.

> Documentação de projeto: [constituição](.specify/memory/constitution.md) ·
> [especificação](specs/001-gerenciamento-tarefas-api/spec.md) ·
> [plano técnico](specs/001-gerenciamento-tarefas-api/plan.md) ·
> [contrato OpenAPI de referência](specs/001-gerenciamento-tarefas-api/contracts/openapi.yaml)

## Stack

| Área | Tecnologia |
|------|------------|
| Linguagem / runtime | Java 21 (LTS) |
| Framework | Spring Boot 4.1.1 (Web MVC, Validation, Data JPA, Security OAuth2 Resource Server, Actuator) |
| Banco | H2 em modo arquivo (persistente) + Flyway |
| Identidade | Keycloak 26 (login, senha e cadastro de usuários ficam no Keycloak) |
| Documentação | springdoc-openapi 3 (Swagger UI) |
| Boilerplate | Lombok + MapStruct |
| Testes | JUnit 5, Mockito, AssertJ, spring-security-test, Testcontainers (Keycloak real), JaCoCo com **100 %** de linhas e ramos |
| Entrega | Docker (multi-stage, usuário não-root) + Docker Compose |

## Subindo tudo com Docker Compose

Pré-requisitos: Docker 27+ com Compose. Portas livres: `8080` (API) e `8180` (Keycloak).

```bash
docker compose up --build
# porta do host ocupada? use outra:
APP_PORTA=8081 docker compose up --build
```

| Serviço | URL |
|---------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Contrato OpenAPI | http://localhost:8080/v3/api-docs |
| Saúde | http://localhost:8080/actuator/health |
| Keycloak (admin / admin) | http://localhost:8180 |

O realm `tarefas` é importado automaticamente de [`keycloak/realm-export.json`](keycloak/realm-export.json)
com os usuários de desenvolvimento `ana` e `bruno` (senha `senha123`).
### Persistência dos dados

Tudo fica na pasta **`./dados`** do projeto (ignorada pelo Git), montada nos containers como bind mount:

| Caminho | Conteúdo |
|---------|----------|
| `./dados/tarefas.mv.db` | Banco H2 da API — o **mesmo arquivo** é usado pela execução local (perfil `dev`) e pelo container |
| `./dados/keycloak/` | Banco embutido do Keycloak (usuários, senhas e seus identificadores `sub`) |

Os dados sobrevivem a `docker compose stop/start`, `down/up` e até `down -v`. Para recomeçar do zero,
apague a pasta `./dados` (com os containers parados). Atenção: as tarefas pertencem ao `sub` do usuário no
Keycloak — se apagar só `./dados/keycloak`, o realm é reimportado com novos identificadores e as tarefas
antigas ficam invisíveis.

## Executando localmente pelo VS Code

Com a extensão **Extension Pack for Java**, abra *Run and Debug* (`F5`) e escolha
**"API local (dev + H2 em ./dados) — sobe Keycloak antes"**: a tarefa `keycloak: subir` executa
`docker compose up -d --wait keycloak` e a API inicia com o perfil `dev` na porta **8081**
(`SERVER_PORT` no `.vscode/launch.json`), gravando em `./dados/tarefas.mv.db`.

- Swagger: http://localhost:8081/swagger-ui.html
- Console H2 (só no perfil `dev`): http://localhost:8081/h2-console — JDBC URL `jdbc:h2:file:./dados/tarefas`, usuário `sa`, senha vazia
- Tarefas úteis (`Terminal > Run Task`): `keycloak: token da ana`, `compose: subir tudo`, `compose: parar tudo`, `maven: verify`

Não execute a API local e o container da API ao mesmo tempo: o H2 em modo arquivo aceita um único processo.

## Obtendo um token

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/tarefas/protocol/openid-connect/token \
  -d grant_type=password -d client_id=tarefas-teste -d username=ana -d password=senha123 | jq -r .access_token)
```

No Swagger UI, clique em **Authorize** e cole o token em `bearerAuth`.

## Endpoints (`/api/v1/tarefas`)

| Método | Caminho | Descrição |
|--------|---------|-----------|
| `GET` | `/` | Lista completa por posição; `?situacao=TODAS\|ABERTAS\|CONCLUIDAS&busca=texto` (busca ignora acentos e maiúsculas) |
| `POST` | `/` | Cadastra tarefa (data de abertura automática, situação `ABERTA`, última posição) |
| `GET` | `/{id}` | Obtém uma tarefa |
| `PUT` | `/{id}` | Edita descrição, prioridade, prazo e valor (gera histórico por campo alterado) |
| `PATCH` | `/{id}/concluir` | Marca como concluída (registra data real) |
| `PATCH` | `/{id}/reabrir` | Reabre (apaga data real) |
| `PATCH` | `/{id}/mover` | Move para `TOPO`, `FINAL` ou `POSICAO` específica mantendo posições contínuas 1..N |
| `DELETE` | `/{id}` | Exclui definitivamente (histórico removido em cascata) |
| `GET` | `/{id}/historico` | Linha do tempo com frases prontas ("Modificado por Ana em 15/09/2026 - Alterou valor de R$ 50,00 para R$ 70,00") |
| `GET` | `/resumo-financeiro` | Total a gastar no mês corrente (tarefas abertas com prazo no mês) |
| `POST` | `/api/v1/usuarios` (público) | Cria uma conta no Keycloak (nome, sobrenome, e-mail, usuário, senha); novo usuário já recebe o papel `usuario` |

### Integração com o frontend (`../task-frontend`)

O `compose.yaml` cria a rede Docker **`tarefas-rede`** (aliases `keycloak` e `task-backend`) que o
compose do frontend usa como `external`. Suba **primeiro o backend**, depois o frontend. O Keycloak roda
com `KC_HOSTNAME=http://localhost:8180` e backchannel dinâmico: tokens obtidos pelo servidor Next.js via
`http://keycloak:8080` saem com o mesmo `iss` que a API valida. O cadastro usa o cliente confidencial
`tarefas-api` (segredo de desenvolvimento no realm; em outros ambientes defina `KEYCLOAK_ADMIN_CLIENT_SECRET`).

> Alterou `keycloak/realm-export.json`? A importação só acontece quando o realm ainda não existe em
> `./dados/keycloak`. Apague essa pasta (e, para evitar tarefas órfãs, também `./dados/tarefas.mv.db`).

Erros seguem RFC 9457 (`application/problem+json`) com títulos e mensagens em português.
Um passo a passo completo com `curl` está em [`quickstart.md`](specs/001-gerenciamento-tarefas-api/quickstart.md).

## Executando localmente pela linha de comando

```bash
docker compose up -d --wait keycloak                       # somente o Keycloak
SERVER_PORT=8081 ./mvnw spring-boot:run                    # perfil dev (padrão): H2 em ./dados/tarefas
```

## Testes e cobertura

```bash
./mvnw verify
```

Executa testes de unidade (`*Test`) e de integração (`*IT`, que sobem um Keycloak efêmero via
Testcontainers — o Docker precisa estar em execução) e falha se a cobertura JaCoCo ficar abaixo de
100 % em linhas ou ramos. Relatório em `target/site/jacoco/index.html`.

> Se o seu IDE compila automaticamente em `target/classes`, desative o autobuild (já configurado em
> `.vscode/settings.json`) para não sobrescrever as classes geradas pelo MapStruct durante o build Maven.

## Estrutura de pacotes

```
com.task.task_backend
├── config/        SegurancaConfig, OpenApiConfig, RelogioConfig
├── controller/    TarefaController
├── dto/           records de entrada/saída
├── entity/        Tarefa, RegistroHistorico e enums
├── exception/     TratadorGlobalExcecoes (ProblemDetail) e exceções de domínio
├── mapper/        TarefaMapper, RegistroHistoricoMapper (MapStruct)
├── repository/    TarefaRepository, RegistroHistoricoRepository
├── seguranca/     UsuarioAutenticado, ConversorAutoridadesKeycloak
├── service/       TarefaService, HistoricoService, ResumoFinanceiroService
└── util/          NormalizadorTexto, FormatadorHistorico
```

Perfis: `dev` (padrão), `docker`, `test`. Migrações em `src/main/resources/db/migration`.
