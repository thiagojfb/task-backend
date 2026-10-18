package com.task.task_backend.dto;

import com.task.task_backend.entity.TipoRegistro;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/** Evento da linha do tempo de uma tarefa, com frase pronta para exibição. */
@Schema(description = "Evento da linha do tempo da tarefa")
public record RegistroHistoricoRespostaDTO(

        @Schema(description = "Identificador do registro")
        UUID id,

        @Schema(description = "Nome de exibição do autor (ou identificador, se indisponível)", example = "Ana Silva")
        String usuarioNome,

        @Schema(description = "Data e hora do evento", example = "2026-09-15T14:32:10")
        LocalDateTime dataHora,

        @Schema(description = "Tipo do evento", example = "ALTERACAO")
        TipoRegistro tipo,

        @Schema(description = "Campo alterado (nulo em CRIACAO)", example = "valor", nullable = true)
        String campo,

        @Schema(description = "Valor anterior já formatado para exibição", example = "R$ 50,00", nullable = true)
        String valorAnterior,

        @Schema(description = "Novo valor já formatado para exibição", example = "R$ 70,00", nullable = true)
        String valorNovo,

        @Schema(description = "Frase pronta para a interface",
                example = "Modificado por Ana Silva em 15/09/2026 - Alterou valor de R$ 50,00 para R$ 70,00")
        String descricao) {
}
