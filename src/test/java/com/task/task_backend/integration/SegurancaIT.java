package com.task.task_backend.integration;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Segurança (integração com Keycloak)")
class SegurancaIT extends KeycloakContainerSuporte {

    @Test
    @DisplayName("Sem token deve responder 401 com ProblemDetail em português")
    void semTokenDeveResponder401() throws Exception {
        mockMvc.perform(get("/api/v1/tarefas"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.detail").value("É necessário um token de acesso válido."));
    }

    @Test
    @DisplayName("Token inválido deve responder 401")
    void tokenInvalidoDeveResponder401() throws Exception {
        mockMvc.perform(get("/api/v1/tarefas").header(AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }

    @Test
    @DisplayName("Usuário sem a role usuario deve responder 403")
    void usuarioSemRoleDeveResponder403() throws Exception {
        mockMvc.perform(get("/api/v1/tarefas").header(AUTHORIZATION, bearer(VISITANTE)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para acessar este recurso."));
    }

    @Test
    @DisplayName("Rota fora da API deve ser negada mesmo com token válido")
    void rotaDesconhecidaDeveSerNegada() throws Exception {
        mockMvc.perform(get("/qualquer-coisa").header(AUTHORIZATION, bearer(ANA)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Caminho do console H2 deve ser público (fora do perfil dev não há console, logo 404)")
    void consoleH2DeveSerPublico() throws Exception {
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Health do Actuator deve ser público")
    void healthDeveSerPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
