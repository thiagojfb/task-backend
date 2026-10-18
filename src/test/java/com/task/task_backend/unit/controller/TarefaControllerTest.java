package com.task.task_backend.unit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.task.task_backend.config.SegurancaConfig;
import com.task.task_backend.controller.TarefaController;
import com.task.task_backend.dto.RegistroHistoricoRespostaDTO;
import com.task.task_backend.dto.ResumoFinanceiroRespostaDTO;
import com.task.task_backend.dto.TarefaRespostaDTO;
import com.task.task_backend.entity.FiltroSituacao;
import com.task.task_backend.entity.Prioridade;
import com.task.task_backend.entity.Situacao;
import com.task.task_backend.entity.TipoRegistro;
import com.task.task_backend.exception.PosicaoInvalidaException;
import com.task.task_backend.exception.TarefaNaoEncontradaException;
import com.task.task_backend.exception.TratadorGlobalExcecoes;
import com.task.task_backend.seguranca.ConversorAutoridadesKeycloak;
import com.task.task_backend.seguranca.UsuarioAutenticado;
import com.task.task_backend.service.ResumoFinanceiroService;
import com.task.task_backend.service.TarefaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TarefaController.class)
@Import({SegurancaConfig.class, TratadorGlobalExcecoes.class, ConversorAutoridadesKeycloak.class})
@ActiveProfiles("test")
@DisplayName("TarefaController")
class TarefaControllerTest {

    private static final String BASE = "/api/v1/tarefas";
    private static final UUID ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TarefaService tarefaService;

    @MockitoBean
    private ResumoFinanceiroService resumoFinanceiroService;

    @MockitoBean
    private UsuarioAutenticado usuarioAutenticado;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    static JwtRequestPostProcessor comoAna() {
        return jwt()
                .jwt(jwt -> jwt.subject("ana-id").claim("name", "Ana Silva").claim("preferred_username", "ana"))
                .authorities(new SimpleGrantedAuthority("ROLE_usuario"));
    }

    static TarefaRespostaDTO respostaExemplo() {
        return new TarefaRespostaDTO(ID, "Pagar conta de luz", Prioridade.ALTA, Situacao.ABERTA,
                LocalDateTime.of(2026, 9, 19, 10, 15, 30), LocalDate.of(2026, 9, 30), null,
                new BigDecimal("250.00"), 1, false);
    }

    @Nested
    @DisplayName("POST /api/v1/tarefas")
    class Criar {

        @Test
        @DisplayName("Deve criar tarefa e retornar 201 com Location")
        void deveCriarTarefaERetornar201() throws Exception {
            when(tarefaService.criar(any())).thenReturn(respostaExemplo());

            mockMvc.perform(post(BASE).with(comoAna())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"Pagar conta de luz","prioridade":"ALTA",
                                     "dataPrevistaConclusao":"2026-09-30","valor":250.00}"""))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost" + BASE + "/" + ID))
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.descricao").value("Pagar conta de luz"))
                    .andExpect(jsonPath("$.prioridade").value("ALTA"))
                    .andExpect(jsonPath("$.situacao").value("ABERTA"))
                    .andExpect(jsonPath("$.valor").value(250.00))
                    .andExpect(jsonPath("$.posicao").value(1))
                    .andExpect(jsonPath("$.atrasada").value(false));
        }

        @Test
        @DisplayName("Deve rejeitar descrição em branco, prioridade ausente e valor negativo")
        void deveRejeitarCamposInvalidos() throws Exception {
            mockMvc.perform(post(BASE).with(comoAna())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"   ","valor":-1}"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Dados inválidos"))
                    .andExpect(jsonPath("$.detail").value("Um ou mais campos estão inválidos."))
                    .andExpect(jsonPath("$.erros.length()").value(3))
                    .andExpect(jsonPath("$.erros[0].campo").value("descricao"))
                    .andExpect(jsonPath("$.erros[0].mensagem").value("A descrição é obrigatória."))
                    .andExpect(jsonPath("$.erros[1].campo").value("prioridade"))
                    .andExpect(jsonPath("$.erros[1].mensagem").value("A prioridade é obrigatória."))
                    .andExpect(jsonPath("$.erros[2].campo").value("valor"))
                    .andExpect(jsonPath("$.erros[2].mensagem").value("O valor não pode ser negativo."));
        }

        @Test
        @DisplayName("Deve rejeitar descrição longa e valor com mais de duas casas decimais")
        void deveRejeitarDescricaoLongaEValorComTresCasas() throws Exception {
            String descricaoLonga = "x".repeat(501);

            mockMvc.perform(post(BASE).with(comoAna())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"%s","prioridade":"BAIXA","valor":10.123}""".formatted(descricaoLonga)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erros[0].campo").value("descricao"))
                    .andExpect(jsonPath("$.erros[0].mensagem").value("A descrição deve ter no máximo 500 caracteres."))
                    .andExpect(jsonPath("$.erros[1].campo").value("valor"))
                    .andExpect(jsonPath("$.erros[1].mensagem").value("O valor deve ter no máximo duas casas decimais."));
        }

        @Test
        @DisplayName("Deve rejeitar prioridade desconhecida como corpo inválido")
        void deveRejeitarPrioridadeDesconhecida() throws Exception {
            mockMvc.perform(post(BASE).with(comoAna())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"Teste","prioridade":"URGENTE"}"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tarefas")
    class Listar {

        @Test
        @DisplayName("Deve listar tarefas do usuário")
        void deveListarTarefas() throws Exception {
            when(tarefaService.listar(FiltroSituacao.TODAS, null)).thenReturn(List.of(respostaExemplo()));

            mockMvc.perform(get(BASE).with(comoAna()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(ID.toString()))
                    .andExpect(jsonPath("$[0].dataAbertura").value("2026-09-19T10:15:30"));
        }

        @Test
        @DisplayName("Deve repassar filtro de situação e termo de busca ao serviço")
        void deveRepassarFiltroEBusca() throws Exception {
            when(tarefaService.listar(FiltroSituacao.ABERTAS, "lu")).thenReturn(List.of(respostaExemplo()));

            mockMvc.perform(get(BASE).param("situacao", "ABERTAS").param("busca", "lu").with(comoAna()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Filtro de situação desconhecido deve retornar 400 Parâmetro inválido")
        void filtroDesconhecidoDeveRetornar400() throws Exception {
            mockMvc.perform(get(BASE).param("situacao", "ATRASADAS").with(comoAna()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                    .andExpect(jsonPath("$.detail").value("O valor informado para o parâmetro 'situacao' é inválido."));
        }

        @Test
        @DisplayName("Deve retornar o resumo financeiro do mês")
        void deveRetornarResumoFinanceiro() throws Exception {
            when(resumoFinanceiroService.obter())
                    .thenReturn(new ResumoFinanceiroRespostaDTO(9, 2026, new BigDecimal("1250.00"), "R$ 1.250,00"));

            mockMvc.perform(get(BASE + "/resumo-financeiro").with(comoAna()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mes").value(9))
                    .andExpect(jsonPath("$.ano").value(2026))
                    .andExpect(jsonPath("$.totalAGastar").value(1250.00))
                    .andExpect(jsonPath("$.totalFormatado").value("R$ 1.250,00"));
        }

        @Test
        @DisplayName("Deve obter tarefa pelo identificador")
        void deveObterTarefaPeloId() throws Exception {
            when(tarefaService.obter(ID)).thenReturn(respostaExemplo());

            mockMvc.perform(get(BASE + "/" + ID).with(comoAna()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()));
        }

        @Test
        @DisplayName("Deve retornar 404 quando a tarefa não existir")
        void deveRetornar404QuandoTarefaNaoExistir() throws Exception {
            when(tarefaService.obter(ID)).thenThrow(new TarefaNaoEncontradaException());

            mockMvc.perform(get(BASE + "/" + ID).with(comoAna()))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Tarefa não encontrada"))
                    .andExpect(jsonPath("$.detail").value("Não existe tarefa com o identificador informado."));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/tarefas/{id} e PATCH concluir/reabrir")
    class EditarEConcluir {

        @Test
        @DisplayName("Deve editar tarefa e retornar 200")
        void deveEditarTarefa() throws Exception {
            when(tarefaService.editar(eq(ID), any())).thenReturn(respostaExemplo());

            mockMvc.perform(put(BASE + "/" + ID).with(comoAna())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"Pagar conta de luz","prioridade":"ALTA","valor":300.00}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()));
        }

        @Test
        @DisplayName("Deve rejeitar edição inválida com 400")
        void deveRejeitarEdicaoInvalida() throws Exception {
            mockMvc.perform(put(BASE + "/" + ID).with(comoAna())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"","prioridade":"ALTA"}"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Dados inválidos"))
                    .andExpect(jsonPath("$.erros[0].campo").value("descricao"));
        }

        @Test
        @DisplayName("Deve retornar 404 ao editar tarefa inexistente")
        void deveRetornar404AoEditarInexistente() throws Exception {
            when(tarefaService.editar(eq(ID), any())).thenThrow(new TarefaNaoEncontradaException());

            mockMvc.perform(put(BASE + "/" + ID).with(comoAna())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"X","prioridade":"ALTA"}"""))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Tarefa não encontrada"));
        }

        @Test
        @DisplayName("Deve concluir tarefa e retornar 200")
        void deveConcluirTarefa() throws Exception {
            when(tarefaService.concluir(ID)).thenReturn(respostaExemplo());

            mockMvc.perform(patch(BASE + "/" + ID + "/concluir").with(comoAna()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()));
        }

        @Test
        @DisplayName("Deve retornar 404 ao concluir tarefa inexistente")
        void deveRetornar404AoConcluirInexistente() throws Exception {
            when(tarefaService.concluir(ID)).thenThrow(new TarefaNaoEncontradaException());

            mockMvc.perform(patch(BASE + "/" + ID + "/concluir").with(comoAna()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve reabrir tarefa e retornar 200")
        void deveReabrirTarefa() throws Exception {
            when(tarefaService.reabrir(ID)).thenReturn(respostaExemplo());

            mockMvc.perform(patch(BASE + "/" + ID + "/reabrir").with(comoAna()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("ABERTA"));
        }

        @Test
        @DisplayName("Deve retornar 404 ao reabrir tarefa inexistente")
        void deveRetornar404AoReabrirInexistente() throws Exception {
            when(tarefaService.reabrir(ID)).thenThrow(new TarefaNaoEncontradaException());

            mockMvc.perform(patch(BASE + "/" + ID + "/reabrir").with(comoAna()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/tarefas/{id}/mover")
    class Mover {

        @Test
        @DisplayName("Deve mover tarefa e retornar 200")
        void deveMoverTarefa() throws Exception {
            when(tarefaService.mover(eq(ID), any())).thenReturn(respostaExemplo());

            mockMvc.perform(patch(BASE + "/" + ID + "/mover").with(comoAna())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"destino":"TOPO"}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posicao").value(1));
        }

        @Test
        @DisplayName("Deve rejeitar destino ausente com 400 Dados inválidos")
        void deveRejeitarDestinoAusente() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID + "/mover").with(comoAna())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"posicao":2}"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Dados inválidos"))
                    .andExpect(jsonPath("$.erros[0].campo").value("destino"))
                    .andExpect(jsonPath("$.erros[0].mensagem").value("O destino é obrigatório."));
        }

        @Test
        @DisplayName("Posição fora do intervalo deve retornar 400 Posição inválida")
        void posicaoForaDoIntervaloDeveRetornar400() throws Exception {
            when(tarefaService.mover(eq(ID), any())).thenThrow(new PosicaoInvalidaException(1, 5));

            mockMvc.perform(patch(BASE + "/" + ID + "/mover").with(comoAna())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"destino":"POSICAO","posicao":0}"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Posição inválida"))
                    .andExpect(jsonPath("$.detail").value("A posição deve estar entre 1 e 5."));
        }

        @Test
        @DisplayName("Destino desconhecido deve retornar 400 Corpo da requisição inválido")
        void destinoDesconhecidoDeveRetornar400() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID + "/mover").with(comoAna())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"destino":"MEIO"}"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
        }

        @Test
        @DisplayName("Deve retornar 404 ao mover tarefa inexistente")
        void deveRetornar404AoMoverInexistente() throws Exception {
            when(tarefaService.mover(eq(ID), any())).thenThrow(new TarefaNaoEncontradaException());

            mockMvc.perform(patch(BASE + "/" + ID + "/mover").with(comoAna())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"destino":"FINAL"}"""))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tarefas/{id}/historico")
    class Historico {

        @Test
        @DisplayName("Deve listar o histórico da tarefa")
        void deveListarHistorico() throws Exception {
            RegistroHistoricoRespostaDTO registro = new RegistroHistoricoRespostaDTO(UUID.randomUUID(), "Ana Silva",
                    LocalDateTime.of(2026, 9, 15, 14, 32, 10), TipoRegistro.ALTERACAO, "valor", "R$ 50,00",
                    "R$ 70,00", "Modificado por Ana Silva em 15/09/2026 - Alterou valor de R$ 50,00 para R$ 70,00");
            when(tarefaService.listarHistorico(ID)).thenReturn(List.of(registro));

            mockMvc.perform(get(BASE + "/" + ID + "/historico").with(comoAna()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].tipo").value("ALTERACAO"))
                    .andExpect(jsonPath("$[0].usuarioNome").value("Ana Silva"))
                    .andExpect(jsonPath("$[0].descricao").value(registro.descricao()));
        }

        @Test
        @DisplayName("Deve retornar 404 para histórico de tarefa inexistente")
        void deveRetornar404ParaHistoricoInexistente() throws Exception {
            when(tarefaService.listarHistorico(ID)).thenThrow(new TarefaNaoEncontradaException());

            mockMvc.perform(get(BASE + "/" + ID + "/historico").with(comoAna()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Tarefa não encontrada"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/tarefas/{id}")
    class Excluir {

        @Test
        @DisplayName("Deve excluir tarefa e retornar 204 sem corpo")
        void deveExcluirTarefa() throws Exception {
            mockMvc.perform(delete(BASE + "/" + ID).with(comoAna()))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(tarefaService).excluir(ID);
        }

        @Test
        @DisplayName("Deve retornar 404 ao excluir tarefa inexistente")
        void deveRetornar404AoExcluirInexistente() throws Exception {
            doThrow(new TarefaNaoEncontradaException()).when(tarefaService).excluir(ID);

            mockMvc.perform(delete(BASE + "/" + ID).with(comoAna()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Tarefa não encontrada"));
        }
    }

    @Nested
    @DisplayName("Segurança")
    class Seguranca {

        @Test
        @DisplayName("Deve responder 401 sem token")
        void deveResponder401SemToken() throws Exception {
            mockMvc.perform(get(BASE))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Não autenticado"));
        }

        @Test
        @DisplayName("Deve responder 403 com token sem a role usuario")
        void deveResponder403SemRole() throws Exception {
            mockMvc.perform(get(BASE).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_outra"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Acesso negado"));
        }
    }
}
