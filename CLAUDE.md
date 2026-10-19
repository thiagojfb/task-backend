<!-- SPECKIT START -->
Para contexto sobre tecnologias, estrutura do projeto, comandos de shell e demais
informações importantes, leia o plano atual da feature:

- Plano ativo: `specs/001-gerenciamento-tarefas-api/plan.md`
- Especificação: `specs/001-gerenciamento-tarefas-api/spec.md`
- Decisões técnicas: `specs/001-gerenciamento-tarefas-api/research.md`
- Modelo de dados: `specs/001-gerenciamento-tarefas-api/data-model.md`
- Contrato REST: `specs/001-gerenciamento-tarefas-api/contracts/openapi.yaml`
- Constituição (fonte de verdade): `.specify/memory/constitution.md`

Regras inegociáveis: todo código, mensagens, logs, Swagger, documentação e comunicação
no terminal em português do Brasil; TDD com cobertura JaCoCo 100% (linhas e ramos);
Java 21 + Spring Boot 4.1.1; Keycloak para identidade; H2 em arquivo + Flyway; Lombok;
arquitetura controller → service → repository com DTOs `record`.
<!-- SPECKIT END -->
