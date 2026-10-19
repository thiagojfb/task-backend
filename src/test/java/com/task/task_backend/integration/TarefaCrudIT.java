package com.task.task_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.task.task_backend.config.RelogioConfig;
import com.task.task_backend.repository.RegistroHistoricoRepository;
import com.task.task_backend.repository.TarefaRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("Tarefas: cadastro, consulta, edição, conclusão e exclusão (integração)")
class TarefaCrudIT extends KeycloakContainerSuporte {

    private static final String BASE = "/api/v1/tarefas";
    private static String tokenAna;
    private static String tokenBruno;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private RegistroHistoricoRepository historicoRepository;

    @BeforeAll
    static void obterTokens() {
        tokenAna = bearer(ANA);
        tokenBruno = bearer(BRUNO);
    }

    @AfterEach
    void limparBanco() {
        historicoRepository.deleteAll();
        tarefaRepository.deleteAll();
    }

    @Nested
    @DisplayName("US1 - Cadastrar e visualizar")
    class CadastrarEVisualizar {

        @Test
        @DisplayName("Deve cadastrar tarefa completa com data de abertura automática, situação aberta e posição 1")
        void deveCadastrarTarefaCompleta() throws Exception {
            LocalDateTime antes = LocalDateTime.now(RelogioConfig.FUSO_BRASILIA).minusMinutes(1);

            MvcResult resultado = mockMvc.perform(post(BASE).header(AUTHORIZATION, tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"Pagar conta de luz","prioridade":"ALTA",
                                     "dataPrevistaConclusao":"2026-09-30","valor":250.00}"""))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.descricao").value("Pagar conta de luz"))
                    .andExpect(jsonPath("$.prioridade").value("ALTA"))
                    .andExpect(jsonPath("$.situacao").value("ABERTA"))
                    .andExpect(jsonPath("$.dataPrevistaConclusao").value("2026-09-30"))
                    .andExpect(jsonPath("$.valor").value(250.00))
                    .andExpect(jsonPath("$.posicao").value(1))
                    .andExpect(jsonPath("$.dataConclusaoReal").isEmpty())
                    .andReturn();

            String dataAbertura = JsonPath.read(corpo(resultado), "$.dataAbertura");
            assertThat(LocalDateTime.parse(dataAbertura)).isAfter(antes);
        }

        @Test
        @DisplayName("Terceira tarefa deve ocupar a posição 3 sem alterar as anteriores")
        void terceiraTarefaDeveOcuparPosicao3() throws Exception {
            criar(tokenAna, "Primeira", "ALTA");
            criar(tokenAna, "Segunda", "MEDIA");

            mockMvc.perform(post(BASE).header(AUTHORIZATION, tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"Terceira","prioridade":"BAIXA"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.posicao").value(3));

            mockMvc.perform(get(BASE).header(AUTHORIZATION, tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].descricao").value("Primeira"))
                    .andExpect(jsonPath("$[0].posicao").value(1))
                    .andExpect(jsonPath("$[1].descricao").value("Segunda"))
                    .andExpect(jsonPath("$[1].posicao").value(2))
                    .andExpect(jsonPath("$[2].descricao").value("Terceira"))
                    .andExpect(jsonPath("$[2].posicao").value(3));
        }

        @Test
        @DisplayName("Tarefa sem valor deve ser criada com R$ 0,00")
        void tarefaSemValorDeveSerCriadaComZero() throws Exception {
            mockMvc.perform(post(BASE).header(AUTHORIZATION, tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"Ler livro","prioridade":"BAIXA"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.valor").value(0.00));
        }

        @Test
        @DisplayName("Cadastro inválido deve retornar 400 com erros em português")
        void cadastroInvalidoDeveRetornar400() throws Exception {
            mockMvc.perform(post(BASE).header(AUTHORIZATION, tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":""}"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Dados inválidos"))
                    .andExpect(jsonPath("$.erros[*].mensagem",
                            hasItems("A descrição é obrigatória.", "A prioridade é obrigatória.")));
        }

        @Test
        @DisplayName("Sem token nenhuma operação deve ser permitida")
        void semTokenNenhumaOperacaoPermitida() throws Exception {
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"X","prioridade":"ALTA"}"""))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
            assertThat(tarefaRepository.count()).isZero();
        }

        @Test
        @DisplayName("Cada usuário deve ver somente as próprias tarefas")
        void cadaUsuarioDeveVerSomenteAsProprias() throws Exception {
            String idDaAna = criar(tokenAna, "Tarefa da Ana", "ALTA");

            mockMvc.perform(get(BASE).header(AUTHORIZATION, tokenBruno))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
            mockMvc.perform(get(BASE + "/" + idDaAna).header(AUTHORIZATION, tokenBruno))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Tarefa não encontrada"));
            mockMvc.perform(get(BASE + "/" + idDaAna).header(AUTHORIZATION, tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.descricao").value("Tarefa da Ana"));
        }
    }

    @Nested
    @DisplayName("US2 - Editar e concluir")
    class EditarEConcluir {

        @Test
        @DisplayName("Alterar o valor deve manter os demais campos")
        void alterarValorDeveManterDemaisCampos() throws Exception {
            String id = criarCompleta(tokenAna, "Pagar conta de luz", "ALTA", "2026-09-30", "50.00");

            mockMvc.perform(put(BASE + "/" + id).header(AUTHORIZATION, tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"Pagar conta de luz","prioridade":"ALTA",
                                     "dataPrevistaConclusao":"2026-09-30","valor":70.00}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valor").value(70.00))
                    .andExpect(jsonPath("$.descricao").value("Pagar conta de luz"))
                    .andExpect(jsonPath("$.prioridade").value("ALTA"))
                    .andExpect(jsonPath("$.dataPrevistaConclusao").value("2026-09-30"))
                    .andExpect(jsonPath("$.posicao").value(1))
                    .andExpect(jsonPath("$.situacao").value("ABERTA"));

            mockMvc.perform(get(BASE + "/" + id).header(AUTHORIZATION, tokenAna))
                    .andExpect(jsonPath("$.valor").value(70.00));
        }

        @Test
        @DisplayName("Concluir deve registrar a data real e mover para o final; reabrir apaga a data e mantém a posição")
        void concluirEReabrir() throws Exception {
            String id = criar(tokenAna, "Ler livro", "BAIXA");
            criar(tokenAna, "Segunda", "MEDIA");
            criar(tokenAna, "Terceira", "ALTA");

            mockMvc.perform(patch(BASE + "/" + id + "/concluir").header(AUTHORIZATION, tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("CONCLUIDA"))
                    .andExpect(jsonPath("$.dataConclusaoReal").isNotEmpty())
                    .andExpect(jsonPath("$.posicao").value(3));

            mockMvc.perform(get(BASE).header(AUTHORIZATION, tokenAna))
                    .andExpect(jsonPath("$[0].descricao").value("Segunda"))
                    .andExpect(jsonPath("$[0].posicao").value(1))
                    .andExpect(jsonPath("$[1].descricao").value("Terceira"))
                    .andExpect(jsonPath("$[1].posicao").value(2))
                    .andExpect(jsonPath("$[2].descricao").value("Ler livro"))
                    .andExpect(jsonPath("$[2].posicao").value(3));

            mockMvc.perform(patch(BASE + "/" + id + "/reabrir").header(AUTHORIZATION, tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("ABERTA"))
                    .andExpect(jsonPath("$.dataConclusaoReal").isEmpty())
                    .andExpect(jsonPath("$.posicao").value(3));
        }

        @Test
        @DisplayName("Duas edições em sequência: a última prevalece e ambas geram histórico")
        void duasEdicoesEmSequencia() throws Exception {
            String id = criarCompleta(tokenAna, "Conta", "ALTA", null, "50.00");
            editarValor(id, "60.00");
            editarValor(id, "80.00");

            mockMvc.perform(get(BASE + "/" + id).header(AUTHORIZATION, tokenAna))
                    .andExpect(jsonPath("$.valor").value(80.00));
            assertThat(historicoRepository.findAll())
                    .filteredOn(registro -> "valor".equals(registro.getCampo()))
                    .extracting(registro -> registro.getValorAnterior() + " -> " + registro.getValorNovo())
                    .containsExactlyInAnyOrder("R$ 50,00 -> R$ 60,00", "R$ 60,00 -> R$ 80,00");
        }

        @Test
        @DisplayName("Editar tarefa de outro usuário deve responder 404")
        void editarTarefaDeOutroUsuarioDeveResponder404() throws Exception {
            String idDaAna = criar(tokenAna, "Da Ana", "ALTA");

            mockMvc.perform(put(BASE + "/" + idDaAna).header(AUTHORIZATION, tokenBruno)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"Invasão","prioridade":"ALTA"}"""))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Tarefa não encontrada"));

            mockMvc.perform(get(BASE + "/" + idDaAna).header(AUTHORIZATION, tokenAna))
                    .andExpect(jsonPath("$.descricao").value("Da Ana"));
        }

        @Test
        @DisplayName("Editar identificador inexistente deve responder 404")
        void editarInexistenteDeveResponder404() throws Exception {
            mockMvc.perform(put(BASE + "/" + UUID.randomUUID()).header(AUTHORIZATION, tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"X","prioridade":"ALTA"}"""))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Não existe tarefa com o identificador informado."));
        }

        private void editarValor(String id, String valor) throws Exception {
            mockMvc.perform(put(BASE + "/" + id).header(AUTHORIZATION, tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"Conta","prioridade":"ALTA","valor":%s}""".formatted(valor)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("US5 - Excluir")
    class Excluir {

        @Test
        @DisplayName("Excluir a do meio deve remover a tarefa e fechar a lacuna de posições")
        void excluirDoMeioDeveFecharLacuna() throws Exception {
            criar(tokenAna, "Primeira", "ALTA");
            String meio = criar(tokenAna, "Segunda", "MEDIA");
            criar(tokenAna, "Terceira", "BAIXA");

            mockMvc.perform(delete(BASE + "/" + meio).header(AUTHORIZATION, tokenAna))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(BASE).header(AUTHORIZATION, tokenAna))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].descricao").value("Primeira"))
                    .andExpect(jsonPath("$[0].posicao").value(1))
                    .andExpect(jsonPath("$[1].descricao").value("Terceira"))
                    .andExpect(jsonPath("$[1].posicao").value(2));
        }

        @Test
        @DisplayName("Histórico e edição de tarefa excluída devem responder 404 e o histórico deve sumir do banco")
        void historicoEEdicaoDeExcluidaDevemResponder404() throws Exception {
            String id = criar(tokenAna, "Efêmera", "BAIXA");
            assertThat(historicoRepository.count()).isEqualTo(1);

            mockMvc.perform(delete(BASE + "/" + id).header(AUTHORIZATION, tokenAna))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(BASE + "/" + id + "/historico").header(AUTHORIZATION, tokenAna))
                    .andExpect(status().isNotFound());
            mockMvc.perform(put(BASE + "/" + id).header(AUTHORIZATION, tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"X","prioridade":"ALTA"}"""))
                    .andExpect(status().isNotFound());
            assertThat(historicoRepository.count()).isZero();
        }

        @Test
        @DisplayName("Excluir tarefa de outro usuário deve responder 404 e manter a tarefa")
        void excluirTarefaDeOutroUsuarioDeveResponder404() throws Exception {
            String idDaAna = criar(tokenAna, "Da Ana", "ALTA");

            mockMvc.perform(delete(BASE + "/" + idDaAna).header(AUTHORIZATION, tokenBruno))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Tarefa não encontrada"));

            mockMvc.perform(get(BASE + "/" + idDaAna).header(AUTHORIZATION, tokenAna))
                    .andExpect(status().isOk());
        }
    }

    /** Cria uma tarefa com todos os campos e devolve o identificador. */
    String criarCompleta(String token, String descricao, String prioridade, String prazo, String valor)
            throws Exception {
        String prazoJson = prazo == null ? "null" : "\"" + prazo + "\"";
        MvcResult resultado = mockMvc.perform(post(BASE).header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"%s","prioridade":"%s","dataPrevistaConclusao":%s,"valor":%s}"""
                                .formatted(descricao, prioridade, prazoJson, valor)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(corpo(resultado), "$.id");
    }

    /** Cria uma tarefa simples e devolve o identificador. */
    String criar(String token, String descricao, String prioridade) throws Exception {
        MvcResult resultado = mockMvc.perform(post(BASE).header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"%s","prioridade":"%s"}""".formatted(descricao, prioridade)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(corpo(resultado), "$.id");
    }

    static String corpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
