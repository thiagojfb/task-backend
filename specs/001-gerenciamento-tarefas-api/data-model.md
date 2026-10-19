# Modelo de Dados: API de Gerenciamento de Tarefas

**Feature**: `001-gerenciamento-tarefas-api` | **Data**: 2026-09-19
**Fonte**: `spec.md` (Key Entities, FR-004..FR-023) e `research.md` (R-07..R-11).

## Diagrama

```text
┌──────────────────────────────┐        ┌──────────────────────────────┐
│ tarefa                       │ 1    * │ registro_historico           │
├──────────────────────────────┤────────┤──────────────────────────────┤
│ id (PK, UUID)                │        │ id (PK, UUID)                │
│ usuario_id (idx)             │        │ tarefa_id (FK, CASCADE, idx) │
│ descricao                    │        │ usuario_id                   │
│ descricao_normalizada (idx)  │        │ usuario_nome                 │
│ prioridade                   │        │ data_hora                    │
│ situacao                     │        │ tipo                         │
│ data_abertura                │        │ campo                        │
│ data_prevista_conclusao      │        │ valor_anterior               │
│ data_conclusao_real          │        │ valor_novo                   │
│ valor                        │        └──────────────────────────────┘
│ posicao (idx c/ usuario_id)  │
└──────────────────────────────┘
```

O **Usuário** não é persistido: `usuario_id` é o claim `sub` do JWT emitido pelo
Keycloak; `usuario_nome` é copiado para o histórico no momento do evento (snapshot),
pois nomes podem mudar no IdP.

## Entidade `Tarefa`

Tabela `tarefa`.

| Campo (Java) | Coluna | Tipo | Regras |
|--------------|--------|------|--------|
| `id` | `id` | `UUID` PK | gerado pela aplicação (`UUID.randomUUID()`) |
| `usuarioId` | `usuario_id` | `VARCHAR(255)` NOT NULL | `sub` do token; imutável |
| `descricao` | `descricao` | `VARCHAR(500)` NOT NULL | 1–500 chars após `trim`; não só espaços (FR-004) |
| `descricaoNormalizada` | `descricao_normalizada` | `VARCHAR(500)` NOT NULL | derivada em `@PrePersist/@PreUpdate` (R-09) |
| `prioridade` | `prioridade` | `VARCHAR(10)` NOT NULL | enum `Prioridade` (`ALTA`, `MEDIA`, `BAIXA`) |
| `situacao` | `situacao` | `VARCHAR(10)` NOT NULL | enum `Situacao` (`ABERTA`, `CONCLUIDA`); inicia `ABERTA` (FR-007) |
| `dataAbertura` | `data_abertura` | `TIMESTAMP` NOT NULL | `LocalDateTime.now(clock)` na criação; imutável (FR-005) |
| `dataPrevistaConclusao` | `data_prevista_conclusao` | `DATE` NULL | prazo informado pelo usuário; pode estar no passado |
| `dataConclusaoReal` | `data_conclusao_real` | `TIMESTAMP` NULL | preenchida ao concluir, apagada ao reabrir (FR-011) |
| `valor` | `valor` | `DECIMAL(15,2)` NOT NULL | ≥ 0, escala 2; padrão `0.00` (FR-004) |
| `posicao` | `posicao` | `INT` NOT NULL | 1..N contínuo por `usuario_id` (FR-014); sem UNIQUE (R-08) |

Índices: `idx_tarefa_usuario_posicao (usuario_id, posicao)`,
`idx_tarefa_usuario_normalizada (usuario_id, descricao_normalizada)`.

Campo derivado (não persistido, exposto no DTO): `atrasada = situacao == ABERTA &&
dataPrevistaConclusao != null && dataPrevistaConclusao < LocalDate.now(clock)`
(FR-008).

Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`;
`@EqualsAndHashCode(onlyExplicitlyIncluded = true)` com `id` incluído — **sem `@Data`**
(Princípio VIII).

### Transições de situação

```text
            concluir()                 reabrir()
 ABERTA ───────────────────▶ CONCLUIDA ───────────────▶ ABERTA
   │  dataConclusaoReal = now(clock)      dataConclusaoReal = null
   │  histórico: CONCLUSAO                histórico: REABERTURA
   └── edição/mover permitidos em ambos os estados (clarificação Q4)
```

`concluir()` em tarefa já concluída e `reabrir()` em tarefa já aberta são operações sem
efeito (idempotentes, HTTP 200, nenhum registro de histórico).

## Entidade `RegistroHistorico`

Tabela `registro_historico`. Imutável após criação (sem setters públicos além do builder).

| Campo (Java) | Coluna | Tipo | Regras |
|--------------|--------|------|--------|
| `id` | `id` | `UUID` PK | gerado pela aplicação |
| `tarefa` | `tarefa_id` | `UUID` FK → `tarefa.id` ON DELETE CASCADE NOT NULL | `@ManyToOne(fetch = LAZY)` |
| `usuarioId` | `usuario_id` | `VARCHAR(255)` NOT NULL | autor (`sub`) |
| `usuarioNome` | `usuario_nome` | `VARCHAR(255)` NOT NULL | `name` → `preferred_username` → `sub` (R-06) |
| `dataHora` | `data_hora` | `TIMESTAMP` NOT NULL | `LocalDateTime.now(clock)` |
| `tipo` | `tipo` | `VARCHAR(12)` NOT NULL | enum `TipoRegistro` (`CRIACAO`, `ALTERACAO`, `CONCLUSAO`, `REABERTURA`) |
| `campo` | `campo` | `VARCHAR(30)` NULL | nome do campo em português: `descricao`, `prioridade`, `dataPrevistaConclusao`, `valor`, `situacao`; NULL em `CRIACAO` |
| `valorAnterior` | `valor_anterior` | `VARCHAR(600)` NULL | já formatado para exibição (FR-019); NULL em `CRIACAO` |
| `valorNovo` | `valor_novo` | `VARCHAR(600)` NULL | idem |

Índice: `idx_historico_tarefa_data (tarefa_id, data_hora DESC)` para FR-018.

### Regras de geração (FR-016, FR-017)

| Operação | Registros gerados |
|----------|-------------------|
| Criar tarefa | 1 × `CRIACAO` (campo/valores nulos) |
| Editar (PUT) | 1 × `ALTERACAO` **por campo** cujo valor mudou; 0 se nada mudou |
| Concluir | 1 × `CONCLUSAO` (`campo=situacao`, `Aberta` → `Concluída`) se estava aberta |
| Reabrir | 1 × `REABERTURA` (`campo=situacao`, `Concluída` → `Aberta`) se estava concluída |
| Mover | nenhum |
| Excluir | nenhum; registros existentes removidos em cascata |

Formatação dos valores (utilitário `FormatadorHistorico`):
- `valor` → `R$ 1.250,00` (espaço comum, separador de milhar `.`, decimal `,`);
- `dataPrevistaConclusao` → `dd/MM/yyyy`; nulo → `—`;
- `prioridade` → `Alta` / `Média` / `Baixa`; `situacao` → `Aberta` / `Concluída`;
- `descricao` → texto literal.

## Enumerações

| Enum | Valores | Rótulo pt-BR |
|------|---------|--------------|
| `Prioridade` | `ALTA`, `MEDIA`, `BAIXA` | Alta, Média, Baixa |
| `Situacao` | `ABERTA`, `CONCLUIDA` | Aberta, Concluída |
| `TipoRegistro` | `CRIACAO`, `ALTERACAO`, `CONCLUSAO`, `REABERTURA` | — |
| `DestinoMovimento` | `TOPO`, `FINAL`, `POSICAO` | — (entrada da API) |
| `FiltroSituacao` | `TODAS`, `ABERTAS`, `CONCLUIDAS` | — (query param) |

Cada enum com rótulo possui método `getRotulo()`; serialização JSON usa o nome da
constante (`ALTA`), rótulo é usado apenas no histórico.

## Migração Flyway

`src/main/resources/db/migration/V1__criar_tabelas_tarefa_e_historico.sql`

```sql
CREATE TABLE tarefa (
    id                        UUID          NOT NULL,
    usuario_id                VARCHAR(255)  NOT NULL,
    descricao                 VARCHAR(500)  NOT NULL,
    descricao_normalizada     VARCHAR(500)  NOT NULL,
    prioridade                VARCHAR(10)   NOT NULL,
    situacao                  VARCHAR(10)   NOT NULL,
    data_abertura             TIMESTAMP     NOT NULL,
    data_prevista_conclusao   DATE,
    data_conclusao_real       TIMESTAMP,
    valor                     DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    posicao                   INT           NOT NULL,
    CONSTRAINT pk_tarefa PRIMARY KEY (id),
    CONSTRAINT ck_tarefa_valor_nao_negativo CHECK (valor >= 0),
    CONSTRAINT ck_tarefa_posicao_positiva CHECK (posicao >= 1)
);

CREATE INDEX idx_tarefa_usuario_posicao ON tarefa (usuario_id, posicao);
CREATE INDEX idx_tarefa_usuario_normalizada ON tarefa (usuario_id, descricao_normalizada);

CREATE TABLE registro_historico (
    id              UUID         NOT NULL,
    tarefa_id       UUID         NOT NULL,
    usuario_id      VARCHAR(255) NOT NULL,
    usuario_nome    VARCHAR(255) NOT NULL,
    data_hora       TIMESTAMP    NOT NULL,
    tipo            VARCHAR(12)  NOT NULL,
    campo           VARCHAR(30),
    valor_anterior  VARCHAR(600),
    valor_novo      VARCHAR(600),
    CONSTRAINT pk_registro_historico PRIMARY KEY (id),
    CONSTRAINT fk_historico_tarefa FOREIGN KEY (tarefa_id)
        REFERENCES tarefa (id) ON DELETE CASCADE
);

CREATE INDEX idx_historico_tarefa_data ON registro_historico (tarefa_id, data_hora DESC);
```

## Consultas do repositório (Spring Data JPA)

`TarefaRepository`:
- `List<Tarefa> findByUsuarioIdOrderByPosicaoAsc(String usuarioId)`
- `List<Tarefa> findByUsuarioIdAndSituacaoOrderByPosicaoAsc(String usuarioId, Situacao situacao)`
- `@Query` busca: `WHERE t.usuarioId = :u AND (:situacao IS NULL OR t.situacao = :situacao) AND t.descricaoNormalizada LIKE CONCAT('%', :termo, '%') ORDER BY t.posicao`
- `Optional<Tarefa> findByIdAndUsuarioId(UUID id, String usuarioId)` (FR-002/FR-003)
- `@Lock(PESSIMISTIC_WRITE) Optional<Tarefa> buscarParaMover(UUID id, String usuarioId)`
- `int countByUsuarioId(String usuarioId)`
- `@Modifying int incrementarPosicoes(String usuarioId, int inicio, int fim)`
- `@Modifying int decrementarPosicoes(String usuarioId, int inicio, int fim)`
- `@Modifying int decrementarPosicoesApos(String usuarioId, int posicao)`
- `@Query` soma: `SELECT COALESCE(SUM(t.valor), 0) FROM Tarefa t WHERE t.usuarioId = :u AND t.situacao = 'ABERTA' AND t.dataPrevistaConclusao BETWEEN :inicio AND :fim` (FR-023)

`RegistroHistoricoRepository`:
- `List<RegistroHistorico> findByTarefaIdOrderByDataHoraDesc(UUID tarefaId)`

## DTOs (records)

| DTO | Direção | Campos |
|-----|---------|--------|
| `TarefaRequisicaoDTO` | entrada (POST/PUT) | `descricao` (`@NotBlank @Size(max=500)`), `prioridade` (`@NotNull`), `dataPrevistaConclusao` (opcional), `valor` (`@DecimalMin("0.00") @Digits(integer=13, fraction=2)`, nulo → 0) |
| `MoverTarefaRequisicaoDTO` | entrada (PATCH mover) | `destino` (`@NotNull`), `posicao` (`@Positive`, obrigatório se `POSICAO`) |
| `TarefaRespostaDTO` | saída | `id, descricao, prioridade, situacao, dataAbertura, dataPrevistaConclusao, dataConclusaoReal, valor, posicao, atrasada` |
| `RegistroHistoricoRespostaDTO` | saída | `id, usuarioNome, dataHora, tipo, campo, valorAnterior, valorNovo, descricao` (frase pronta: "Modificado por Ana em 15/09/2026 - Alterou valor de R$ 50,00 para R$ 70,00") |
| `ResumoFinanceiroRespostaDTO` | saída | `mes` (1–12), `ano`, `totalAGastar` (BigDecimal), `totalFormatado` ("R$ 1.250,00") |
| `ErroValidacaoDTO` | saída (dentro do `ProblemDetail`) | `campo`, `mensagem` |

Todas as mensagens de validação em português, declaradas nas anotações.
