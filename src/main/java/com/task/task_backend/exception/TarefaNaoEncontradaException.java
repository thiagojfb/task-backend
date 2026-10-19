package com.task.task_backend.exception;

/** Tarefa inexistente ou pertencente a outro usuário (tratada como inexistente). */
public class TarefaNaoEncontradaException extends RuntimeException {

    public TarefaNaoEncontradaException() {
        super("Não existe tarefa com o identificador informado.");
    }
}
