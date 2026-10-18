-- Preferências de interface por usuário (identificado pelo claim "sub" do token Keycloak)
CREATE TABLE preferencia_usuario (
    usuario_id     VARCHAR(255) NOT NULL,
    filtro         VARCHAR(10)  NOT NULL,
    atualizado_em  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_preferencia_usuario PRIMARY KEY (usuario_id)
);
