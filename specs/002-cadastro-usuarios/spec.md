# Feature Specification: Cadastro de Usuários

**Feature Branch**: `002-cadastro-usuarios` (implementada sobre `001-gerenciamento-tarefas-api` por
ainda não haver commit da feature anterior; separar ao commitar)

**Created**: 2026-09-20

**Status**: Implementada

**Input**: "É preciso ter na tela de login um link também para cadastrar novo usuário. Esse usuário
deverá ir para o keycloak. [...] ao cadastrar, quero que seja aproveitada a própria tela de login
para inserir o usuário e senha para que seja enviado o novo usuário para o keycloak."

## Contexto e Escopo

O frontend terá uma tela própria de login/cadastro. O cadastro precisa criar o usuário no provedor de
identidade (Keycloak). Pela constituição (Princípio IV), essa operação pertence à API e DEVE usar a
Keycloak Admin REST API — nunca uma tabela local de usuários.

**Dentro do escopo**: endpoint público de cadastro; validação dos dados; criação do usuário no
Keycloak com senha definitiva e papel `usuario`; tratamento de usuário/e-mail duplicado.

**Fora do escopo**: confirmação de e-mail, recuperação de senha, edição de perfil, papéis
administrativos.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Criar conta pela tela de login (Priority: P1)

Um visitante informa nome, sobrenome, e-mail, nome de usuário e senha. O sistema cria a conta no
Keycloak, já habilitada e com o papel `usuario`, e o visitante consegue entrar imediatamente com as
credenciais informadas.

**Independent Test**: cadastrar um usuário novo, obter token por login e senha e listar tarefas
(200 com lista vazia).

**Acceptance Scenarios**:

1. **Given** dados válidos, **When** o visitante cadastra, **Then** recebe 201 com usuário, e-mail e
   nome, e consegue autenticar-se em seguida.
2. **Given** nome de usuário ou e-mail já existente, **When** cadastra, **Then** recebe 409
   "Usuário já cadastrado".
3. **Given** senha com menos de 8 caracteres, e-mail inválido ou campos em branco, **When** cadastra,
   **Then** recebe 400 com os campos e mensagens em português.
4. **Given** o provedor de identidade indisponível, **When** cadastra, **Then** recebe 502
   "Não foi possível cadastrar o usuário no provedor de identidade" sem vazar detalhes internos.
5. **Given** um cadastro bem-sucedido, **When** o usuário autentica, **Then** o token contém o papel
   `usuario` e o nome de exibição "Nome Sobrenome".

### Edge Cases

- Cadastro não exige autenticação (é a única rota pública de `/api`), mas é rate-limit-agnostic nesta
  fase (uso pessoal).
- Nome de usuário é normalizado para minúsculas pelo Keycloak; a API devolve o valor como enviado.

## Requirements *(mandatory)*

- **FR-101**: O sistema DEVE expor `POST /api/v1/usuarios` acessível sem autenticação.
- **FR-102**: O sistema DEVE validar: nome (1–100), sobrenome (1–100), e-mail válido (≤ 255),
  usuário (3–50, letras/números/`._-`), senha (8–128).
- **FR-103**: O sistema DEVE criar o usuário no Keycloak via Admin REST API autenticando-se com um
  cliente confidencial de service account (`client_credentials`).
- **FR-104**: O usuário criado DEVE estar habilitado, com e-mail marcado como verificado, senha não
  temporária e papel `usuario` (via default role do realm).
- **FR-105**: Conflito no Keycloak (409) DEVE ser convertido em 409 `ProblemDetail`
  "Usuário já cadastrado"; outras falhas em 502 "Falha no provedor de identidade".
- **FR-106**: O segredo do cliente DEVE vir de variável de ambiente; nunca do código.

### Key Entities

- **Usuário (Keycloak)**: username, email, firstName, lastName, enabled, emailVerified, credentials.
  Não persistido pela API.

## Success Criteria *(mandatory)*

- **SC-101**: 100 % dos cadastros válidos resultam em login bem-sucedido em seguida.
- **SC-102**: Duplicidades e dados inválidos nunca chegam a criar usuário (0 registros parciais).
- **SC-103**: Toda mensagem de erro em português; cobertura de testes mantida em 100 %.
