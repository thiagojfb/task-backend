# Implementation Plan: Preferências do Usuário

**Date**: 2026-09-20 | **Spec**: [spec.md](spec.md)

## Design

```
db/migration/V2__criar_tabela_preferencia_usuario.sql
entity/PreferenciaUsuario            usuarioId (PK), filtro (FiltroSituacao, STRING), atualizadoEm
repository/PreferenciaUsuarioRepository  JpaRepository<PreferenciaUsuario, String>
dto/PreferenciasDTO                  record(@NotNull FiltroSituacao filtro) — entrada e saída
mapper/PreferenciaMapper             MapStruct: paraResposta(entidade)
service/PreferenciaService           obter() [padrão TODAS], salvar(dto) [upsert, atualizadoEm = now(clock)]
controller/PreferenciaController     GET/PUT /api/v1/preferencias (tag "Preferências")
```

## Constitution Check

| # | Portão | Status |
|---|--------|--------|
| I | Camadas + DTO record + MapStruct | ✅ |
| II | Português | ✅ |
| III | Testes primeiro; 100 % | ✅ |
| IV | Rota sob `/api/**` → exige `usuario`; isolamento por `sub` | ✅ |
| V | OpenAPI em português; contrato de referência atualizado | ✅ |
| VI | Flyway V2; `ddl-auto=validate` | ✅ |

## Tasks

- [x] T201 Testes: `PreferenciaServiceTest`, `PreferenciaControllerTest`, `PreferenciaUsuarioRepositoryIT`, `PreferenciaIT`
- [x] T202 Implementação: migração V2, entidade, repositório, DTO, mapper, serviço, controller, tag OpenAPI
- [x] T203 Contrato `contracts/openapi.yaml` (feature 001) e README; `./mvnw verify` 100 %
