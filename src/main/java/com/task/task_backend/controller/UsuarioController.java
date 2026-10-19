package com.task.task_backend.controller;

import com.task.task_backend.config.OpenApiConfig;
import com.task.task_backend.dto.CadastroUsuarioRequisicaoDTO;
import com.task.task_backend.dto.UsuarioCadastradoRespostaDTO;
import com.task.task_backend.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro público de usuários (única rota de /api sem autenticação). */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_USUARIOS)
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Cria uma nova conta de usuário",
            description = "Cria o usuário no Keycloak, já habilitado e com o papel usuario. Não exige autenticação. "
                    + "Após o cadastro, o usuário pode entrar com nome de usuário e senha.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Nome de usuário ou e-mail já cadastrado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "Falha ao comunicar com o provedor de identidade",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public UsuarioCadastradoRespostaDTO cadastrar(@Valid @RequestBody CadastroUsuarioRequisicaoDTO requisicao) {
        return usuarioService.cadastrar(requisicao);
    }
}
