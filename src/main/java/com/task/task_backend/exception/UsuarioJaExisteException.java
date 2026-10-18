package com.task.task_backend.exception;

/** Já existe usuário com o mesmo nome de usuário ou e-mail no provedor de identidade. */
public class UsuarioJaExisteException extends RuntimeException {

    public UsuarioJaExisteException() {
        super("Já existe um usuário com este nome de usuário ou e-mail.");
    }
}
