# Implementation Plan: Cadastro de Usuários

**Date**: 2026-09-20 | **Spec**: [spec.md](spec.md) | **Base**: plano da feature 001

## Summary

Endpoint público `POST /api/v1/usuarios` que cria o usuário no Keycloak pela Admin REST API usando
`RestClient` e um cliente confidencial de service account (`tarefas-api`, roles
`realm-management: manage-users, view-users`). O realm passa a conceder `usuario` como default role,
então todo usuário novo já acessa a API. O Keycloak recebe `KC_HOSTNAME=http://localhost:8180` e
`KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true` para que tokens obtidos pela rede interna (servidor Next.js)
tenham o mesmo `iss` validado pela API. Uma rede Docker compartilhada (`tarefas-rede`) conecta os
containers do backend e do frontend.

## Technical Context

Sem novas dependências: `RestClient` (Spring Web), `@ConfigurationProperties`, Bean Validation.
Testes: `MockRestServiceServer` para o cliente HTTP; `@WebMvcTest`; IT com Keycloak real.

## Constitution Check

| # | Portão | Status |
|---|--------|--------|
| I | `UsuarioController → UsuarioService → KeycloakAdminCliente` (integração externa em `seguranca/`); DTOs `record`; `ProblemDetail` | ✅ |
| II | Tudo em português | ✅ |
| III | Testes primeiro; cobertura 100 % mantida | ✅ |
| IV | Cadastro via Admin REST API; rota pública explicitamente listada; segredo por variável de ambiente | ✅ |
| V | `@Operation`/`@ApiResponses` em português; sem `bearerAuth` na operação pública | ✅ |
| VI–VIII | Sem impacto em persistência; Compose atualizado (env + rede); `record` + construtor | ✅ |

## Design

```
config/KeycloakAdminPropriedades      @ConfigurationProperties("keycloak.admin"): url, realm, clientId, clientSecret
seguranca/KeycloakAdminCliente        obterTokenServico() [client_credentials] + criarUsuario(NovoUsuarioKeycloak)
seguranca/NovoUsuarioKeycloak         record(username, email, firstName, lastName, senha)
dto/CadastroUsuarioRequisicaoDTO      record validado (nome, sobrenome, email, usuario, senha)
dto/UsuarioCadastradoRespostaDTO      record(usuario, email, nome)
exception/UsuarioJaExisteException    → 409 "Usuário já cadastrado"
exception/FalhaProvedorIdentidadeException → 502 "Falha no provedor de identidade"
service/UsuarioService                cadastrar(dto)
controller/UsuarioController          POST /api/v1/usuarios → 201
```

Configuração (`application.yml`):

```yaml
keycloak:
  admin:
    url: ${KEYCLOAK_ADMIN_URL:http://localhost:8180}
    realm: tarefas
    client-id: tarefas-api
    client-secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET:tarefas-api-secret-dev}   # valor de desenvolvimento
```

Compose (backend): Keycloak com `KC_HOSTNAME`/`KC_HOSTNAME_BACKCHANNEL_DYNAMIC`; `app` com
`KEYCLOAK_ADMIN_URL=http://keycloak:8080` e `KEYCLOAK_ADMIN_CLIENT_SECRET`; rede `tarefas-rede`
(nome fixo) com aliases `keycloak` e `task-backend`.
