package com.task.task_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Item da lista de erros de validação retornada dentro do ProblemDetail. */
@Schema(description = "Campo inválido e respectiva mensagem")
public record ErroValidacaoDTO(
        @Schema(description = "Nome do campo inválido", example = "descricao") String campo,
        @Schema(description = "Motivo da rejeição", example = "A descrição é obrigatória.") String mensagem) {
}
