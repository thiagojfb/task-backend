# Guia Rápido: API de Gerenciamento de Tarefas

**Feature**: `001-gerenciamento-tarefas-api` | **Data**: 2026-09-19

Este guia valida a feature de ponta a ponta após a implementação. Todos os comandos
assumem a raiz do repositório e Git Bash / PowerShell no Windows.

## Pré-requisitos

- Java 21 (`java -version`)
- Docker 27+ com Docker Compose (`docker compose version`)
- Portas livres: `8080` (aplicação) e `8180` (Keycloak)

## 1. Subir tudo com Docker Compose

```bash
docker compose up --build
# Se a porta 8080 já estiver em uso na sua máquina:
APP_PORTA=8081 docker compose up --build
```

Aguarde as mensagens:
- Keycloak: `Realm 'tarefas' imported` e `Listening on: http://0.0.0.0:8080`
- Aplicação: `Started TaskBackendApplication`

Serviços disponíveis:

| Serviço | URL |
|---------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Contrato OpenAPI | http://localhost:8080/v3/api-docs |
| Saúde | http://localhost:8080/actuator/health |
| Keycloak Admin | http://localhost:8180 (admin / admin) |

Usuários de desenvolvimento (realm `tarefas`): `ana` / `senha123` e `bruno` / `senha123`.

## 2. Obter um token

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/tarefas/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=tarefas-teste" \
  -d "username=ana" -d "password=senha123" | jq -r .access_token)
echo $TOKEN
```

Ou, no Swagger UI, clique em **Authorize** e cole o token em `bearerAuth`.

## 3. Fluxo completo (validação dos cenários da spec)

> No Git Bash do Windows, corpos JSON com acentos passados via `-d '...'` podem ser corrompidos pelo
> terminal. Nesses casos salve o JSON em arquivo UTF-8 e use `--data-binary @arquivo.json`.
> Ajuste `dataPrevistaConclusao` para uma data do mês corrente ao validar o resumo financeiro.

```bash
API=http://localhost:8080/api/v1/tarefas
AUTH="Authorization: Bearer $TOKEN"
CT="Content-Type: application/json"

# US1 — criar três tarefas
curl -s -H "$AUTH" -H "$CT" -X POST $API -d '{"descricao":"Pagar conta de luz","prioridade":"ALTA","dataPrevistaConclusao":"2026-09-30","valor":250.00}'
curl -s -H "$AUTH" -H "$CT" -X POST $API -d '{"descricao":"Comprar lâmpada","prioridade":"MEDIA","valor":45.90}'
curl -s -H "$AUTH" -H "$CT" -X POST $API -d '{"descricao":"Ler livro","prioridade":"BAIXA"}'

# US1 — listar (posições 1,2,3; terceira com valor 0.00)
curl -s -H "$AUTH" $API | jq '.[] | {posicao, descricao, valor, atrasada}'

# guardar id da primeira tarefa
ID=$(curl -s -H "$AUTH" $API | jq -r '.[0].id')

# US2 — editar valor (gera histórico "R$ 250,00" → "R$ 300,00")
curl -s -H "$AUTH" -H "$CT" -X PUT $API/$ID -d '{"descricao":"Pagar conta de luz","prioridade":"ALTA","dataPrevistaConclusao":"2026-09-30","valor":300.00}'

# US2 — concluir e reabrir
curl -s -H "$AUTH" -X PATCH $API/$ID/concluir | jq '{situacao, dataConclusaoReal}'
curl -s -H "$AUTH" -X PATCH $API/$ID/reabrir  | jq '{situacao, dataConclusaoReal}'

# US3 — mover para o final, depois para a posição 2, depois topo
curl -s -H "$AUTH" -H "$CT" -X PATCH $API/$ID/mover -d '{"destino":"FINAL"}' | jq .posicao
curl -s -H "$AUTH" -H "$CT" -X PATCH $API/$ID/mover -d '{"destino":"POSICAO","posicao":2}' | jq .posicao
curl -s -H "$AUTH" -H "$CT" -X PATCH $API/$ID/mover -d '{"destino":"TOPO"}' | jq .posicao
# posição inválida → 400 "A posição deve estar entre 1 e 3."
curl -s -H "$AUTH" -H "$CT" -X PATCH $API/$ID/mover -d '{"destino":"POSICAO","posicao":9}'

# US4 — histórico (mais recente primeiro: REABERTURA, CONCLUSAO, ALTERACAO valor, CRIACAO)
curl -s -H "$AUTH" $API/$ID/historico | jq '.[] | .descricao'

# US6 — filtros, busca sem acento e resumo financeiro
curl -s -H "$AUTH" "$API?situacao=ABERTAS" | jq length
curl -s -H "$AUTH" "$API?busca=lampada" | jq '.[].descricao'      # → "Comprar lâmpada"
curl -s -H "$AUTH" $API/resumo-financeiro                          # → totalFormatado "R$ 300,00"

# US5 — excluir e verificar posições contínuas
curl -s -o /dev/null -w "%{http_code}\n" -H "$AUTH" -X DELETE $API/$ID   # 204
curl -s -H "$AUTH" $API | jq '.[].posicao'                              # → 1, 2
curl -s -H "$AUTH" $API/$ID/historico -w "%{http_code}\n"               # 404

# Segurança — sem token → 401; token de bruno não vê tarefas de ana
curl -s -o /dev/null -w "%{http_code}\n" $API
```

## 4. Executar os testes com cobertura

```bash
./mvnw verify
```

Critérios de sucesso (Princípio III):
- `BUILD SUCCESS`;
- testes de unidade (`*Test`) e de integração (`*IT`, com Keycloak via Testcontainers) verdes;
- `jacoco:check` sem violações — relatório em `target/site/jacoco/index.html` com 100% de
  linhas e ramos (exclusões: `TaskBackendApplication`, `mapper/*Impl`).

## 5. Executar localmente sem Docker para a aplicação

Somente o Keycloak em container, aplicação via Maven ou pelo launcher do VS Code
(`F5` → "API local (dev + H2 em ./dados) — sobe Keycloak antes"). O H2 é o mesmo arquivo usado pelo
container (`./dados/tarefas.mv.db`); não rode os dois ao mesmo tempo.

```bash
docker compose up -d --wait keycloak
SERVER_PORT=8081 ./mvnw spring-boot:run
```

Console H2 (apenas `dev`): http://localhost:8080/h2-console — JDBC URL
`jdbc:h2:file:./dados/tarefas`, usuário `sa`, senha vazia.

## 6. Encerrar

```bash
docker compose down          # os dados ficam em ./dados (tarefas.mv.db e keycloak/) — persistem sempre
rm -rf dados                 # somente se quiser recomeçar do zero (com os containers parados)
```

> As tarefas pertencem ao identificador (`sub`) do usuário no Keycloak. Se apenas `./dados/keycloak`
> for apagado, o realm é reimportado com novos identificadores e as tarefas antigas deixam de ser
> visíveis — apague a pasta `./dados` inteira ou nenhuma das duas.
