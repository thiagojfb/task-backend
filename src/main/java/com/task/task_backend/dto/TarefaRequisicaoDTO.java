package com.task.task_backend.dto;

import com.task.task_backend.entity.Prioridade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados para cadastrar ou editar uma tarefa (formulário unificado). */
@Schema(description = "Dados para cadastrar ou editar uma tarefa")
public record TarefaRequisicaoDTO(

        @Schema(description = "Descrição da tarefa (obrigatória, não pode ser só espaços)",
                example = "Pagar conta de luz", maxLength = 500)
        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String descricao,

        @Schema(description = "Prioridade (Alta = vermelho, Média = amarelo, Baixa = azul)", example = "ALTA")
        @NotNull(message = "A prioridade é obrigatória.")
        Prioridade prioridade,

        @Schema(description = "Prazo previsto de conclusão (pode ser anterior à data atual)",
                example = "2026-09-30", nullable = true)
        LocalDate dataPrevistaConclusao,

        @Schema(description = "Valor em reais a gastar; ausente equivale a 0,00", example = "250.00",
                nullable = true)
        @DecimalMin(value = "0.00", message = "O valor não pode ser negativo.")
        @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo duas casas decimais.")
        BigDecimal valor) {
}
