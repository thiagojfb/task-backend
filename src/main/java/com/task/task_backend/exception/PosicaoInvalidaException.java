package com.task.task_backend.exception;

/** Posição de destino fora do intervalo válido da lista do usuário. */
public class PosicaoInvalidaException extends RuntimeException {

    public PosicaoInvalidaException(int minimo, int maximo) {
        super("A posição deve estar entre %d e %d.".formatted(minimo, maximo));
    }
}
