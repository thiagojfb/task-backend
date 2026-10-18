package com.task.task_backend.entity;

import java.util.Optional;

/** Filtro rápido da listagem (barra lateral): todas, abertas ou concluídas. */
public enum FiltroSituacao {
    TODAS,
    ABERTAS,
    CONCLUIDAS;

    /** Converte o filtro na situação correspondente; {@code TODAS} não restringe. */
    public Optional<Situacao> paraSituacao() {
        return switch (this) {
            case TODAS -> Optional.empty();
            case ABERTAS -> Optional.of(Situacao.ABERTA);
            case CONCLUIDAS -> Optional.of(Situacao.CONCLUIDA);
        };
    }
}
