package com.task.task_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Confirmação de cadastro; a senha nunca é retornada. */
@Schema(description = "Usuário criado no provedor de identidade")
public record UsuarioCadastradoRespostaDTO(
        @Schema(description = "Nome de usuário para login", example = "carla") String usuario,
        @Schema(description = "E-mail", example = "carla@exemplo.com.br") String email,
        @Schema(description = "Nome completo de exibição", example = "Carla Dias") String nome) {
}
