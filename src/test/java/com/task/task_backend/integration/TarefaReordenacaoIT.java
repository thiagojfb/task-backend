package com.task.task_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.task.task_backend.repository.RegistroHistoricoRepository;
import com.task.task_backend.repository.TarefaRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("Tarefas: reordenação (integração)")
class TarefaReordenacaoIT extends KeycloakContainerSuporte {

    private static final String BASE = "/api/v1/tarefas";
    private static String tokenAna;
    private static String tokenBruno;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private RegistroHistoricoRepository historicoRepository;

    /** Identificadores das tarefas de Ana na ordem original de criação (índice 0 = posição 1). */
    private List<String> ids;

    @BeforeAll
    static void obterTokens() {
        tokenAna = bearer(ANA);
        tokenBruno = bearer(BRUNO);
    }

    @BeforeEach
    void criarCincoTarefas() throws Exception {
        ids = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ids.add(criar(tokenAna, "T" + i));
        }
        criar(tokenBruno, "B1");
        criar(tokenBruno, "B2");
    }

    @AfterEach
    void limparBanco() {
        historicoRepository.deleteAll();
        tarefaRepository.deleteAll();
    }

    @Test
    @DisplayName("Mover a quinta para o topo deve resultar em [5,1,2,3,4]")
    void moverQuintaParaTopo() throws Exception {
        mover(ids.get(4), """
                {"destino":"TOPO"}""").andExpect(status().isOk()).andExpect(jsonPath("$.posicao").value(1));

        assertThat(ordemAtual()).containsExactly("T5", "T1", "T2", "T3", "T4");
    }

    @Test
    @DisplayName("Mover a primeira para o final deve resultar em [2,3,4,5,1]")
    void moverPrimeiraParaFinal() throws Exception {
        mover(ids.get(0), """
                {"destino":"FINAL"}""").andExpect(status().isOk()).andExpect(jsonPath("$.posicao").value(5));

        assertThat(ordemAtual()).containsExactly("T2", "T3", "T4", "T5", "T1");
    }

    @Test
    @DisplayName("Mover a quarta para a posição 2 deve resultar em [1,4,2,3,5]")
    void moverQuartaParaPosicao2() throws Exception {
        mover(ids.get(3), """
                {"destino":"POSICAO","posicao":2}""").andExpect(status().isOk()).andExpect(jsonPath("$.posicao").value(2));

        assertThat(ordemAtual()).containsExactly("T1", "T4", "T2", "T3", "T5");
    }

    @Test
    @DisplayName("Posições 9 e 0 devem ser rejeitadas com a mensagem do intervalo válido")
    void posicoesForaDoIntervaloDevemSerRejeitadas() throws Exception {
        for (int posicao : new int[] {9, 0}) {
            mover(ids.get(0), """
                    {"destino":"POSICAO","posicao":%d}""".formatted(posicao))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Posição inválida"))
                    .andExpect(jsonPath("$.detail").value("A posição deve estar entre 1 e 5."));
        }
        assertThat(ordemAtual()).containsExactly("T1", "T2", "T3", "T4", "T5");
    }

    @Test
    @DisplayName("Mover para a própria posição deve ser sem efeito e sem erro")
    void moverParaPropriaPosicaoDeveSerSemEfeito() throws Exception {
        mover(ids.get(2), """
                {"destino":"POSICAO","posicao":3}""").andExpect(status().isOk()).andExpect(jsonPath("$.posicao").value(3));

        assertThat(ordemAtual()).containsExactly("T1", "T2", "T3", "T4", "T5");
    }

    @Test
    @DisplayName("Reordenação não deve gerar registros de histórico")
    void reordenacaoNaoDeveGerarHistorico() throws Exception {
        long registrosAntes = historicoRepository.count();

        mover(ids.get(4), """
                {"destino":"TOPO"}""").andExpect(status().isOk());

        assertThat(historicoRepository.count()).isEqualTo(registrosAntes);
    }

    @Test
    @DisplayName("Propriedade: após 30 movimentos aleatórios as posições permanecem contínuas de 1 a N")
    void posicoesPermanecemContinuasAposMovimentosAleatorios() throws Exception {
        Random aleatorio = new Random(42);
        String[] destinos = {"TOPO", "FINAL", "POSICAO"};

        for (int i = 0; i < 30; i++) {
            String id = ids.get(aleatorio.nextInt(ids.size()));
            String destino = destinos[aleatorio.nextInt(destinos.length)];
            String corpo = "POSICAO".equals(destino)
                    ? """
                    {"destino":"POSICAO","posicao":%d}""".formatted(1 + aleatorio.nextInt(5))
                    : """
                    {"destino":"%s"}""".formatted(destino);
            mover(id, corpo).andExpect(status().isOk());

            List<Integer> posicoes = posicoesAtuais(tokenAna);
            assertThat(posicoes).containsExactlyElementsOf(IntStream.rangeClosed(1, 5).boxed().toList());
        }

        assertThat(ordemDe(tokenBruno)).containsExactly("B1", "B2");
        assertThat(posicoesAtuais(tokenBruno)).containsExactly(1, 2);
    }

    private org.springframework.test.web.servlet.ResultActions mover(String id, String corpo) throws Exception {
        return mockMvc.perform(patch(BASE + "/" + id + "/mover").header(AUTHORIZATION, tokenAna)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private List<String> ordemAtual() throws Exception {
        return ordemDe(tokenAna);
    }

    private List<String> ordemDe(String token) throws Exception {
        String corpo = mockMvc.perform(get(BASE).header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(corpo, "$[*].descricao");
    }

    private List<Integer> posicoesAtuais(String token) throws Exception {
        String corpo = mockMvc.perform(get(BASE).header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(corpo, "$[*].posicao");
    }

    private String criar(String token, String descricao) throws Exception {
        String corpo = mockMvc.perform(post(BASE).header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"%s","prioridade":"MEDIA"}""".formatted(descricao)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(corpo, "$.id");
    }
}
