# Tasks: Cadastro de Usuários

- [x] T101 Realm: default role com `usuario`; cliente confidencial `tarefas-api` (service account, `manage-users`/`view-users`); `directAccessGrantsEnabled` no `tarefas-frontend` — `keycloak/realm-export.json`
- [x] T102 [P] Teste `TEST/unit/seguranca/KeycloakAdminClienteTest.java` (MockRestServiceServer): token + criação 201; 409 → `UsuarioJaExisteException`; 500 → `FalhaProvedorIdentidadeException`; falha ao obter token → `FalhaProvedorIdentidadeException`
- [x] T103 [P] Teste `TEST/unit/service/UsuarioServiceTest.java`: monta `NovoUsuarioKeycloak` e devolve DTO com nome completo
- [x] T104 [P] Teste `TEST/unit/controller/UsuarioControllerTest.java`: 201 sem token; 400 validações em português; 409; 502
- [x] T105 [P] Teste `TEST/integration/UsuarioCadastroIT.java`: cadastro real → login por senha → `GET /api/v1/tarefas` 200; duplicado → 409; nome no token = "Nome Sobrenome"
- [x] T106 Ampliar `TratadorGlobalExcecoesTest` com os dois novos handlers
- [x] T107 Implementar `KeycloakAdminPropriedades`, `NovoUsuarioKeycloak`, `KeycloakAdminCliente`, exceções, DTOs, `UsuarioService`, `UsuarioController`, handlers, `SegurancaConfig` (rota pública), `application*.yml`
- [x] T108 `compose.yaml`: `KC_HOSTNAME`, `KC_HOSTNAME_BACKCHANNEL_DYNAMIC`, env do admin client, rede `tarefas-rede` com aliases; validar `./mvnw verify` 100 % e `docker compose up`
