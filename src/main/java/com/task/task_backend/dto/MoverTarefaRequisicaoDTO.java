package com.task.task_backend.dto;

import com.task.task_backend.entity.DestinoMovimento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Destino da movimentação. A posição é obrigatória apenas quando {@code destino = POSICAO}; o intervalo
 * válido (1..N) é validado pelo serviço, que é a fonte única da mensagem de erro.
 */
@Schema(description = "Destino da movimentação de uma tarefa na lista")
public record MoverTarefaRequisicaoDTO(

        @Schema(description = "TOPO, FINAL ou POSICAO", example = "TOPO")
        @NotNull(message = "O destino é obrigatório.")
        DestinoMovimento destino,

        @Schema(description = "Posição de destino (1..N) quando destino = POSICAO; intervalo validado pelo servidor",
                example = "3", nullable = true)
        Integer posicao) {
}
