-- Tabela principal: tarefas de um usuário (identificado pelo claim "sub" do token Keycloak)
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

-- Histórico imutável de alterações; removido em cascata junto com a tarefa
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
