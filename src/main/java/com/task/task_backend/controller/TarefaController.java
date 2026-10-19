package com.task.task_backend.controller;

import com.task.task_backend.config.OpenApiConfig;
import com.task.task_backend.dto.MoverTarefaRequisicaoDTO;
import com.task.task_backend.dto.RegistroHistoricoRespostaDTO;
import com.task.task_backend.dto.ResumoFinanceiroRespostaDTO;
import com.task.task_backend.dto.TarefaRequisicaoDTO;
import com.task.task_backend.dto.TarefaRespostaDTO;
import com.task.task_backend.entity.FiltroSituacao;
import com.task.task_backend.service.ResumoFinanceiroService;
import com.task.task_backend.service.TarefaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Endpoints REST de tarefas do usuário autenticado. */
@RestController
@RequestMapping("/api/v1/tarefas")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_TAREFAS)
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Token válido sem o papel usuario",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
public class TarefaController {

    private final TarefaService tarefaService;
    private final ResumoFinanceiroService resumoFinanceiroService;

    @PostMapping
    @Operation(summary = "Cadastra uma nova tarefa",
            description = "A data de abertura é gerada automaticamente, a situação inicia como ABERTA e a tarefa "
                    + "ocupa a última posição da lista. Gera registro de histórico CRIACAO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tarefa criada; cabeçalho Location aponta para ela"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TarefaRespostaDTO> criar(@Valid @RequestBody TarefaRequisicaoDTO requisicao) {
        TarefaRespostaDTO resposta = tarefaService.criar(requisicao);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(resposta);
    }

    @GetMapping
    @Operation(summary = "Lista todas as tarefas do usuário autenticado",
            description = "Retorna a lista completa (sem paginação) ordenada pela posição manual, "
                    + "com filtro por situação e busca por trecho da descrição (ignora acentos e maiúsculas).")
    @ApiResponse(responseCode = "200", description = "Lista de tarefas (pode ser vazia)")
    public List<TarefaRespostaDTO> listar(
            @Parameter(description = "Filtro rápido por situação; padrão TODAS")
            @RequestParam(defaultValue = "TODAS") FiltroSituacao situacao,
            @Parameter(description = "Trecho da descrição; ignora maiúsculas/minúsculas e acentos", example = "lampada")
            @RequestParam(required = false) String busca) {
        return tarefaService.listar(situacao, busca);
    }

    @GetMapping("/resumo-financeiro")
    @Operation(summary = "Total a gastar no mês corrente",
            description = "Soma dos valores das tarefas abertas do usuário cuja data prevista de conclusão está dentro "
                    + "do mês corrente (fuso America/Sao_Paulo). Tarefas sem data prevista não entram no cálculo.",
            tags = OpenApiConfig.TAG_FINANCEIRO)
    @ApiResponse(responseCode = "200", description = "Resumo financeiro do mês")
    public ResumoFinanceiroRespostaDTO obterResumoFinanceiro() {
        return resumoFinanceiroService.obter();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtém uma tarefa pelo identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarefa encontrada"),
            @ApiResponse(responseCode = "404", description = "Tarefa inexistente ou de outro usuário",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TarefaRespostaDTO obter(
            @Parameter(description = "Identificador da tarefa") @PathVariable UUID id) {
        return tarefaService.obter(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita os campos de conteúdo de uma tarefa",
            description = "Substitui descrição, prioridade, data prevista de conclusão e valor. Permitido em tarefas "
                    + "abertas ou concluídas. Gera um registro de histórico ALTERACAO por campo efetivamente alterado; "
                    + "nenhum registro se nada mudou.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarefa atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tarefa inexistente ou de outro usuário",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TarefaRespostaDTO editar(
            @Parameter(description = "Identificador da tarefa") @PathVariable UUID id,
            @Valid @RequestBody TarefaRequisicaoDTO requisicao) {
        return tarefaService.editar(id, requisicao);
    }

    @PatchMapping("/{id}/concluir")
    @Operation(summary = "Marca a tarefa como concluída",
            description = "Define situacao = CONCLUIDA, registra dataConclusaoReal e move a tarefa para o final da lista. Idempotente: tarefa já concluída "
                    + "retorna 200 sem gerar histórico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarefa concluída"),
            @ApiResponse(responseCode = "404", description = "Tarefa inexistente ou de outro usuário",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TarefaRespostaDTO concluir(
            @Parameter(description = "Identificador da tarefa") @PathVariable UUID id) {
        return tarefaService.concluir(id);
    }

    @PatchMapping("/{id}/reabrir")
    @Operation(summary = "Reabre uma tarefa concluída",
            description = "Define situacao = ABERTA e apaga dataConclusaoReal. Idempotente: tarefa já aberta "
                    + "retorna 200 sem gerar histórico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarefa reaberta"),
            @ApiResponse(responseCode = "404", description = "Tarefa inexistente ou de outro usuário",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TarefaRespostaDTO reabrir(
            @Parameter(description = "Identificador da tarefa") @PathVariable UUID id) {
        return tarefaService.reabrir(id);
    }

    @PatchMapping("/{id}/mover")
    @Operation(summary = "Move a tarefa para o topo, final ou posição específica",
            description = "Reposiciona a tarefa e desloca as demais para manter a sequência contínua 1..N. "
                    + "Mover para a posição atual é uma operação sem efeito. Não gera histórico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarefa movida"),
            @ApiResponse(responseCode = "400", description = "Posição fora do intervalo válido ou corpo inválido",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tarefa inexistente ou de outro usuário",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TarefaRespostaDTO mover(
            @Parameter(description = "Identificador da tarefa") @PathVariable UUID id,
            @Valid @RequestBody MoverTarefaRequisicaoDTO requisicao) {
        return tarefaService.mover(id, requisicao);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui definitivamente uma tarefa",
            description = "Remove a tarefa e todo o seu histórico; reorganiza as posições restantes.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tarefa excluída"),
            @ApiResponse(responseCode = "404", description = "Tarefa inexistente ou de outro usuário",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void excluir(@Parameter(description = "Identificador da tarefa") @PathVariable UUID id) {
        tarefaService.excluir(id);
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Lista o histórico de alterações da tarefa",
            description = "Registros ordenados do mais recente ao mais antigo, com frase pronta para exibição.",
            tags = OpenApiConfig.TAG_HISTORICO)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Linha do tempo da tarefa"),
            @ApiResponse(responseCode = "404", description = "Tarefa inexistente ou de outro usuário",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<RegistroHistoricoRespostaDTO> listarHistorico(
            @Parameter(description = "Identificador da tarefa") @PathVariable UUID id) {
        return tarefaService.listarHistorico(id);
    }
}
