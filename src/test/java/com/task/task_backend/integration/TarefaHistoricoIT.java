package com.task.task_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.task.task_backend.repository.RegistroHistoricoRepository;
import com.task.task_backend.repository.TarefaRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("Tarefas: histórico de alterações (integração)")
class TarefaHistoricoIT extends KeycloakContainerSuporte {

    private static final String BASE = "/api/v1/tarefas";
    private static final String CORPO_ORIGINAL = """
            {"descricao":"Pagar conta de luz","prioridade":"ALTA","dataPrevistaConclusao":"2026-09-30","valor":50.00}""";
    private static String tokenAna;
    private static String tokenBruno;
    private static String tokenSemNome;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private RegistroHistoricoRepository historicoRepository;

    @BeforeAll
    static void obterTokens() {
        tokenAna = bearer(ANA);
        tokenBruno = bearer(BRUNO);
        tokenSemNome = bearer(SEMNOME);
    }

    @AfterEach
    void limparBanco() {
        historicoRepository.deleteAll();
        tarefaRepository.deleteAll();
    }

    @Test
    @DisplayName("Alterar o valor deve gerar registro com autor, data, campo e valores formatados")
    void alterarValorDeveGerarRegistroFormatado() throws Exception {
        String id = criar(tokenAna, CORPO_ORIGINAL);
        editar(tokenAna, id, """
                {"descricao":"Pagar conta de luz","prioridade":"ALTA","dataPrevistaConclusao":"2026-09-30","valor":70.00}""");
        String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        mockMvc.perform(get(BASE + "/" + id + "/historico").header(AUTHORIZATION, tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("ALTERACAO"))
                .andExpect(jsonPath("$[0].usuarioNome").value("Ana Silva"))
                .andExpect(jsonPath("$[0].campo").value("valor"))
                .andExpect(jsonPath("$[0].valorAnterior").value("R$ 50,00"))
                .andExpect(jsonPath("$[0].valorNovo").value("R$ 70,00"))
                .andExpect(jsonPath("$[0].dataHora").isNotEmpty())
                .andExpect(jsonPath("$[0].descricao")
                        .value("Modificado por Ana Silva em " + hoje + " - Alterou valor de R$ 50,00 para R$ 70,00"))
                .andExpect(jsonPath("$[1].tipo").value("CRIACAO"))
                .andExpect(jsonPath("$[1].descricao").value("Criada por Ana Silva em " + hoje));
    }

    @Test
    @DisplayName("Editar descrição e prioridade juntas deve gerar um registro por campo")
    void editarDoisCamposDeveGerarDoisRegistros() throws Exception {
        String id = criar(tokenAna, CORPO_ORIGINAL);
        editar(tokenAna, id, """
                {"descricao":"Pagar energia","prioridade":"BAIXA","dataPrevistaConclusao":"2026-09-30","valor":50.00}""");

        List<String> campos = camposDoHistorico(id);

        assertThat(campos).containsExactlyInAnyOrder("descricao", "prioridade", null);
        assertThat(campos).hasSize(3);
    }

    @Test
    @DisplayName("Edição sem mudanças não deve gerar registros")
    void edicaoSemMudancasNaoDeveGerarRegistros() throws Exception {
        String id = criar(tokenAna, CORPO_ORIGINAL);

        editar(tokenAna, id, CORPO_ORIGINAL);

        mockMvc.perform(get(BASE + "/" + id + "/historico").header(AUTHORIZATION, tokenAna))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("CRIACAO"));
    }

    @Test
    @DisplayName("Concluir deve registrar mudança de situação Aberta → Concluída")
    void concluirDeveRegistrarMudancaDeSituacao() throws Exception {
        String id = criar(tokenAna, CORPO_ORIGINAL);

        mockMvc.perform(patch(BASE + "/" + id + "/concluir").header(AUTHORIZATION, tokenAna))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/" + id + "/historico").header(AUTHORIZATION, tokenAna))
                .andExpect(jsonPath("$[0].tipo").value("CONCLUSAO"))
                .andExpect(jsonPath("$[0].campo").value("situacao"))
                .andExpect(jsonPath("$[0].valorAnterior").value("Aberta"))
                .andExpect(jsonPath("$[0].valorNovo").value("Concluída"))
                .andExpect(jsonPath("$[0].descricao").value(org.hamcrest.Matchers.startsWith("Concluída por Ana Silva em ")));
    }

    @Test
    @DisplayName("Tarefa recém-criada deve ter exatamente um registro de criação")
    void tarefaRecemCriadaDeveTerUmRegistro() throws Exception {
        String id = criar(tokenAna, CORPO_ORIGINAL);

        mockMvc.perform(get(BASE + "/" + id + "/historico").header(AUTHORIZATION, tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("CRIACAO"))
                .andExpect(jsonPath("$[0].campo").isEmpty())
                .andExpect(jsonPath("$[0].valorAnterior").isEmpty())
                .andExpect(jsonPath("$[0].valorNovo").isEmpty());
    }

    @Test
    @DisplayName("Mover não deve gerar registro de histórico")
    void moverNaoDeveGerarRegistro() throws Exception {
        String primeira = criar(tokenAna, CORPO_ORIGINAL);
        criar(tokenAna, CORPO_ORIGINAL);

        mockMvc.perform(patch(BASE + "/" + primeira + "/mover").header(AUTHORIZATION, tokenAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destino":"FINAL"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/" + primeira + "/historico").header(AUTHORIZATION, tokenAna))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Histórico de tarefa alheia deve responder 404")
    void historicoDeTarefaAlheiaDeveResponder404() throws Exception {
        String idDaAna = criar(tokenAna, CORPO_ORIGINAL);

        mockMvc.perform(get(BASE + "/" + idDaAna + "/historico").header(AUTHORIZATION, tokenBruno))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Tarefa não encontrada"));
    }

    @Test
    @DisplayName("Usuário sem nome de exibição deve aparecer no histórico pelo nome de usuário")
    void usuarioSemNomeDeveAparecerPeloNomeDeUsuario() throws Exception {
        String id = criar(tokenSemNome, CORPO_ORIGINAL);

        mockMvc.perform(get(BASE + "/" + id + "/historico").header(AUTHORIZATION, tokenSemNome))
                .andExpect(jsonPath("$[0].usuarioNome").value("semnome"));
    }

    private String criar(String token, String corpo) throws Exception {
        String resposta = mockMvc.perform(post(BASE).header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(resposta, "$.id");
    }

    private void editar(String token, String id, String corpo) throws Exception {
        mockMvc.perform(put(BASE + "/" + id).header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());
    }

    private List<String> camposDoHistorico(String id) throws Exception {
        String resposta = mockMvc.perform(get(BASE + "/" + id + "/historico").header(AUTHORIZATION, tokenAna))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(resposta, "$[*].campo");
    }
}
