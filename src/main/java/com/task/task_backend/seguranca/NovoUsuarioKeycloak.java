package com.task.task_backend.seguranca;

/** Dados necessários para criar um usuário no Keycloak. */
public record NovoUsuarioKeycloak(String username, String email, String firstName, String lastName, String senha) {
}
