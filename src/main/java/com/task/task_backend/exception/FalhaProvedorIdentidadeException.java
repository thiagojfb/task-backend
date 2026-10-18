package com.task.task_backend.exception;

/** Falha de comunicação ou resposta inesperada do Keycloak ao cadastrar um usuário. */
public class FalhaProvedorIdentidadeException extends RuntimeException {

    public FalhaProvedorIdentidadeException(Throwable causa) {
        super("Não foi possível cadastrar o usuário no provedor de identidade.", causa);
    }
}
