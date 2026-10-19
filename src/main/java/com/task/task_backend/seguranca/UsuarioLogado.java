package com.task.task_backend.seguranca;

/** Identidade mínima do usuário autenticado: identificador único (claim {@code sub}) e nome de exibição. */
public record UsuarioLogado(String id, String nome) {
}
