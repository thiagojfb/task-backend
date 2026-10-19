package com.task.task_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados para criar uma conta de usuário no Keycloak a partir da tela de login. */
@Schema(description = "Dados para criar uma nova conta de usuário")
public record CadastroUsuarioRequisicaoDTO(

        @Schema(description = "Primeiro nome", example = "Carla")
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,

        @Schema(description = "Sobrenome", example = "Dias")
        @NotBlank(message = "O sobrenome é obrigatório.")
        @Size(max = 100, message = "O sobrenome deve ter no máximo 100 caracteres.")
        String sobrenome,

        @Schema(description = "E-mail (usado também para login)", example = "carla@exemplo.com.br")
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail é inválido.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
        String email,

        @Schema(description = "Nome de usuário (3 a 50 caracteres: letras, números, ponto, hífen ou sublinhado)",
                example = "carla")
        @NotBlank(message = "O nome de usuário é obrigatório.")
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
                message = "O nome de usuário deve ter de 3 a 50 caracteres (letras, números, ponto, hífen ou sublinhado).")
        String usuario,

        @Schema(description = "Senha (8 a 128 caracteres)", example = "segredo123", format = "password")
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres.")
        String senha) {
}
