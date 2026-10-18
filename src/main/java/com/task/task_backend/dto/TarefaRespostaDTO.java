package com.task.task_backend.dto;

import com.task.task_backend.entity.Prioridade;
import com.task.task_backend.entity.Situacao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Representação completa de uma tarefa retornada pela API. */
@Schema(description = "Representação completa de uma tarefa")
public record TarefaRespostaDTO(

        @Schema(description = "Identificador da tarefa", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Descrição da tarefa", example = "Pagar conta de luz")
        String descricao,

        @Schema(description = "Prioridade", example = "ALTA")
        Prioridade prioridade,

        @Schema(description = "Situação atual", example = "ABERTA")
        Situacao situacao,

        @Schema(description = "Data/hora de abertura, gerada automaticamente", example = "2026-09-19T10:15:30")
        LocalDateTime dataAbertura,

        @Schema(description = "Prazo previsto de conclusão", example = "2026-09-30", nullable = true)
        LocalDate dataPrevistaConclusao,

        @Schema(description = "Data/hora real em que a tarefa foi concluída", nullable = true)
        LocalDateTime dataConclusaoReal,

        @Schema(description = "Valor em reais com duas casas decimais", example = "250.00")
        BigDecimal valor,

        @Schema(description = "Posição manual na lista (1 = topo)", example = "1", minimum = "1")
        Integer posicao,

        @Schema(description = "Verdadeiro quando aberta e com prazo anterior à data atual", example = "false")
        boolean atrasada) {
}
