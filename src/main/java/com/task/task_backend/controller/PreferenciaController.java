package com.task.task_backend.controller;

import com.task.task_backend.config.OpenApiConfig;
import com.task.task_backend.dto.PreferenciasDTO;
import com.task.task_backend.service.PreferenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Preferências de interface do usuário autenticado (persistidas na API). */
@RestController
@RequestMapping("/api/v1/preferencias")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_PREFERENCIAS)
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Token válido sem o papel usuario",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
public class PreferenciaController {

    private final PreferenciaService preferenciaService;

    @GetMapping
    @Operation(summary = "Obtém as preferências do usuário",
            description = "Devolve o filtro rápido preferido; TODAS quando ainda não houver preferência salva.")
    @ApiResponse(responseCode = "200", description = "Preferências atuais")
    public PreferenciasDTO obter() {
        return preferenciaService.obter();
    }

    @PutMapping
    @Operation(summary = "Salva as preferências do usuário",
            description = "Cria ou atualiza a preferência; vale para qualquer navegador ou dispositivo do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferências salvas"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PreferenciasDTO salvar(@Valid @RequestBody PreferenciasDTO preferencias) {
        return preferenciaService.salvar(preferencias);
    }
}
