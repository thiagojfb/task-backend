package com.task.task_backend.dto;

import com.task.task_backend.entity.FiltroSituacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Preferências de interface do usuário (entrada e saída). */
@Schema(description = "Preferências de interface do usuário autenticado")
public record PreferenciasDTO(
        @Schema(description = "Filtro rápido preferido da lista de tarefas", example = "ABERTAS")
        @NotNull(message = "O filtro é obrigatório.")
        FiltroSituacao filtro) {
}
