package com.task.task_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Indicador "total a gastar neste mês" exibido na barra lateral. */
@Schema(description = "Indicador financeiro do mês corrente")
public record ResumoFinanceiroRespostaDTO(

        @Schema(description = "Mês de referência (1 a 12)", example = "9", minimum = "1", maximum = "12")
        int mes,

        @Schema(description = "Ano de referência", example = "2026")
        int ano,

        @Schema(description = "Soma dos valores das tarefas abertas com prazo no mês", example = "1250.00")
        BigDecimal totalAGastar,

        @Schema(description = "Valor pronto para exibição", example = "R$ 1.250,00")
        String totalFormatado) {
}
