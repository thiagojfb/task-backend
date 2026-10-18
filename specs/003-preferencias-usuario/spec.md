# Feature Specification: Preferências do Usuário

**Created**: 2026-09-20 | **Status**: Implementada | **Base**: feature 001

**Input**: "Todas as vezes que eu logar, carregar minha preferência de filtros. [...] preciso que seja
feita no backend. Em qualquer navegador isso se reflete."

## Escopo

Persistir, por usuário (claim `sub`), o filtro rápido preferido da lista de tarefas
(`TODAS`, `ABERTAS`, `CONCLUIDAS`) para que qualquer dispositivo/navegador restaure a mesma
preferência ao entrar. Fora do escopo: outras preferências (tema, ordenação), sincronização em
tempo real entre abas.

## User Story (P1)

Usuário escolhe um filtro no dashboard; ao entrar em outro navegador, a lista já abre com esse filtro.

**Cenários**: sem preferência salva → `GET` devolve `TODAS`; `PUT {filtro: ABERTAS}` → `GET` devolve
`ABERTAS`; `PUT` novo valor sobrescreve; usuários distintos não enxergam a preferência um do outro;
`PUT` com filtro inválido → 400 em português; sem token → 401.

## Requirements

- **FR-201** `GET /api/v1/preferencias` DEVE devolver `{ filtro }` do usuário autenticado, com `TODAS`
  quando não houver registro.
- **FR-202** `PUT /api/v1/preferencias` DEVE criar ou atualizar (upsert) a preferência do usuário e
  devolver o estado salvo.
- **FR-203** `filtro` DEVE ser validado contra `FiltroSituacao`; valores inválidos → 400.
- **FR-204** A preferência DEVE ser isolada por `usuario_id` (chave primária).

## Key Entities

- **PreferenciaUsuario**: `usuario_id` (PK), `filtro`, `atualizado_em`.

## Success Criteria

- **SC-201** Login em navegador diferente restaura o mesmo filtro (verificável com dois clientes).
- **SC-202** Cobertura mantida em 100 %.
