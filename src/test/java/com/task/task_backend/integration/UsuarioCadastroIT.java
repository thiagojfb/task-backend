package com.task.task_backend.integration;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.task.task_backend.repository.RegistroHistoricoRepository;
import com.task.task_backend.repository.TarefaRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("Cadastro de usuários no Keycloak (integração)")
class UsuarioCadastroIT extends KeycloakContainerSuporte {

    private static final String ROTA = "/api/v1/usuarios";

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private RegistroHistoricoRepository historicoRepository;

    @AfterEach
    void limparBanco() {
        historicoRepository.deleteAll();
        tarefaRepository.deleteAll();
    }

    @Test
    @DisplayName("Usuário cadastrado deve conseguir autenticar, acessar a API e aparecer no histórico pelo nome completo")
    void usuarioCadastradoDeveAutenticarEAcessarApi() throws Exception {
        mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Carla","sobrenome":"Dias","email":"carla@exemplo.com.br",
                                 "usuario":"carla","senha":"segredo123"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value("carla"))
                .andExpect(jsonPath("$.nome").value("Carla Dias"));

        String bearer = "Bearer " + obterToken("carla", "segredo123");

        mockMvc.perform(get("/api/v1/tarefas").header(AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        String corpo = mockMvc.perform(post("/api/v1/tarefas").header(AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Primeira da Carla","prioridade":"MEDIA"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(corpo, "$.id");

        mockMvc.perform(get("/api/v1/tarefas/" + id + "/historico").header(AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[0].usuarioNome").value("Carla Dias"));
    }

    @Test
    @DisplayName("Cadastrar usuário já existente deve responder 409")
    void cadastrarExistenteDeveResponder409() throws Exception {
        mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ana","sobrenome":"Outra","email":"outra@exemplo.com.br",
                                 "usuario":"ana","senha":"segredo123"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Usuário já cadastrado"));
    }
}
