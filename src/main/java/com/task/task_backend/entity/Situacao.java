package com.task.task_backend.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Situação da tarefa no ciclo de vida Aberta ⇄ Concluída. */
@Getter
@RequiredArgsConstructor
public enum Situacao {
    ABERTA("Aberta"),
    CONCLUIDA("Concluída");

    private final String rotulo;
}
