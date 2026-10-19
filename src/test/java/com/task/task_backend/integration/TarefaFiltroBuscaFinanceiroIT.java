package com.task.task_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.task.task_backend.config.RelogioConfig;
import com.task.task_backend.entity.Prioridade;
import com.task.task_backend.entity.Situacao;
import com.task.task_backend.entity.Tarefa;
import com.task.task_backend.repository.RegistroHistoricoRepository;
import com.task.task_backend.repository.TarefaRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("Tarefas: filtros, busca e resumo financeiro (integração)")
class TarefaFiltroBuscaFinanceiroIT extends KeycloakContainerSuporte {

    private static final String BASE = "/api/v1/tarefas";
    private static final YearMonth MES_ATUAL = YearMonth.now(RelogioConfig.FUSO_BRASILIA);
    private static String tokenAna;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private RegistroHistoricoRepository historicoRepository;

    @BeforeAll
    static void obterTokens() {
        tokenAna = bearer(ANA);
    }

    @AfterEach
    void limparBanco() {
        historicoRepository.deleteAll();
        tarefaRepository.deleteAll();
    }

    @Test
    @DisplayName("Filtro ABERTAS deve retornar apenas as abertas em ordem de posição")
    void filtroAbertasDeveRetornarApenasAbertas() throws Exception {
        criar("Aberta 1", null, null);
        String concluida1 = criar("Concluída 1", null, null);
        criar("Aberta 2", null, null);
        String concluida2 = criar("Concluída 2", null, null);
        criar("Aberta 3", null, null);
        concluir(concluida1);
        concluir(concluida2);

        mockMvc.perform(get(BASE).param("situacao", "ABERTAS").header(AUTHORIZATION, tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].descricao").value("Aberta 1"))
                .andExpect(jsonPath("$[1].descricao").value("Aberta 2"))
                .andExpect(jsonPath("$[2].descricao").value("Aberta 3"));

        mockMvc.perform(get(BASE).param("situacao", "CONCLUIDAS").header(AUTHORIZATION, tokenAna))
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get(BASE).param("situacao", "TODAS").header(AUTHORIZATION, tokenAna))
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @DisplayName("Busca deve ignorar maiúsculas e acentos")
    void buscaDeveIgnorarMaiusculasEAcentos() throws Exception {
        criar("Pagar luz", null, null);
        criar("Comprar lâmpada", null, null);

        mockMvc.perform(get(BASE).param("busca", "LU").header(AUTHORIZATION, tokenAna))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].descricao").value("Pagar luz"));
        mockMvc.perform(get(BASE).param("busca", "lampada").header(AUTHORIZATION, tokenAna))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].descricao").value("Comprar lâmpada"));
    }

    @Test
    @DisplayName("Resumo financeiro deve somar apenas tarefas abertas com prazo no mês corrente")
    void resumoDeveSomarApenasAbertasDoMes() throws Exception {
        LocalDate noMes = MES_ATUAL.atDay(1);
        LocalDate mesSeguinte = MES_ATUAL.plusMonths(1).atDay(1);
        criar("Aluguel", noMes, "1000.00");
        criar("Internet", MES_ATUAL.atEndOfMonth(), "250.00");
        String concluida = criar("Já paga", noMes, "500.00");
        criar("Mês que vem", mesSeguinte, "300.00");
        criar("Sem prazo", null, "99.00");
        concluir(concluida);

        mockMvc.perform(get(BASE + "/resumo-financeiro").header(AUTHORIZATION, tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value(MES_ATUAL.getMonthValue()))
                .andExpect(jsonPath("$.ano").value(MES_ATUAL.getYear()))
                .andExpect(jsonPath("$.totalAGastar").value(1250.00))
                .andExpect(jsonPath("$.totalFormatado").value("R$ 1.250,00"));
    }

    @Test
    @DisplayName("Sem tarefas abertas no mês o resumo deve ser R$ 0,00")
    void semAbertasNoMesResumoDeveSerZero() throws Exception {
        mockMvc.perform(get(BASE + "/resumo-financeiro").header(AUTHORIZATION, tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAGastar").value(0.00))
                .andExpect(jsonPath("$.totalFormatado").value("R$ 0,00"));
    }

    @Test
    @DisplayName("Filtro e busca combinados devem respeitar ambos os critérios")
    void filtroEBuscaCombinados() throws Exception {
        criar("Pagar luz", null, null);
        String concluida = criar("Pagar água", null, null);
        criar("Ler livro", null, null);
        concluir(concluida);

        mockMvc.perform(get(BASE).param("situacao", "ABERTAS").param("busca", "pagar").header(AUTHORIZATION, tokenAna))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].descricao").value("Pagar luz"));
        mockMvc.perform(get(BASE).param("situacao", "CONCLUIDAS").param("busca", "pagar").header(AUTHORIZATION, tokenAna))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].descricao").value("Pagar água"));
    }

    @Test
    @DisplayName("Filtro de situação desconhecido deve responder 400 em português")
    void filtroDesconhecidoDeveResponder400() throws Exception {
        mockMvc.perform(get(BASE).param("situacao", "ATRASADAS").header(AUTHORIZATION, tokenAna))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
    }

    @Test
    @DisplayName("SC-006: listar 500 tarefas deve levar menos de 1 segundo")
    void listarQuinhentasTarefasEmMenosDeUmSegundo() throws Exception {
        String usuarioId = tarefaRepository.findAll().stream().findFirst()
                .map(Tarefa::getUsuarioId)
                .orElseGet(() -> {
                    try {
                        criar("Semente", null, null);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                    return tarefaRepository.findAll().get(0).getUsuarioId();
                });
        List<Tarefa> lote = new ArrayList<>();
        IntStream.rangeClosed(2, 500).forEach(posicao -> lote.add(Tarefa.builder()
                .id(UUID.randomUUID())
                .usuarioId(usuarioId)
                .descricao("Tarefa " + posicao)
                .prioridade(Prioridade.BAIXA)
                .situacao(Situacao.ABERTA)
                .dataAbertura(LocalDateTime.now())
                .valor(BigDecimal.ONE)
                .posicao(posicao)
                .build()));
        tarefaRepository.saveAll(lote);

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            String corpo = mockMvc.perform(get(BASE).header(AUTHORIZATION, tokenAna))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            List<Object> itens = JsonPath.read(corpo, "$[*].id");
            assertThat(itens).hasSize(500);
        });
    }

    private String criar(String descricao, LocalDate prazo, String valor) throws Exception {
        String prazoJson = prazo == null ? "null" : "\"" + prazo + "\"";
        String valorJson = valor == null ? "null" : valor;
        String corpo = mockMvc.perform(post(BASE).header(AUTHORIZATION, tokenAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"%s","prioridade":"MEDIA","dataPrevistaConclusao":%s,"valor":%s}"""
                                .formatted(descricao, prazoJson, valorJson)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(corpo, "$.id");
    }

    private void concluir(String id) throws Exception {
        mockMvc.perform(patch(BASE + "/" + id + "/concluir").header(AUTHORIZATION, tokenAna))
                .andExpect(status().isOk());
    }
}
