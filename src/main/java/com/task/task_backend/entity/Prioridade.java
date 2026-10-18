package com.task.task_backend.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Prioridade da tarefa; o rótulo é usado apenas em textos exibidos ao usuário (histórico). */
@Getter
@RequiredArgsConstructor
public enum Prioridade {
    ALTA("Alta"),
    MEDIA("Média"),
    BAIXA("Baixa");

    private final String rotulo;
}
