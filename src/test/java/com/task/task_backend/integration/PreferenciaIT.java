package com.task.task_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.task.task_backend.entity.FiltroSituacao;
import com.task.task_backend.repository.PreferenciaUsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("Preferências do usuário (integração)")
class PreferenciaIT extends KeycloakContainerSuporte {

    private static final String ROTA = "/api/v1/preferencias";

    @Autowired
    private PreferenciaUsuarioRepository repositorio;

    @AfterEach
    void limpar() {
        repositorio.deleteAll();
    }

    @Test
    @DisplayName("Deve devolver TODAS sem preferência, salvar, sobrescrever e isolar por usuário")
    void devePersistirEIsolarPorUsuario() throws Exception {
        String ana = bearer(ANA);
        String bruno = bearer(BRUNO);

        mockMvc.perform(get(ROTA).header(AUTHORIZATION, ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filtro").value("TODAS"));

        mockMvc.perform(put(ROTA).header(AUTHORIZATION, ana).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filtro":"ABERTAS"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filtro").value("ABERTAS"));
        mockMvc.perform(get(ROTA).header(AUTHORIZATION, ana)).andExpect(jsonPath("$.filtro").value("ABERTAS"));

        mockMvc.perform(put(ROTA).header(AUTHORIZATION, ana).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filtro":"CONCLUIDAS"}"""))
                .andExpect(status().isOk());
        mockMvc.perform(get(ROTA).header(AUTHORIZATION, ana)).andExpect(jsonPath("$.filtro").value("CONCLUIDAS"));

        mockMvc.perform(get(ROTA).header(AUTHORIZATION, bruno)).andExpect(jsonPath("$.filtro").value("TODAS"));

        assertThat(repositorio.count()).isEqualTo(1);
        assertThat(repositorio.findAll().get(0).getFiltro()).isEqualTo(FiltroSituacao.CONCLUIDAS);
    }

    @Test
    @DisplayName("Sem token deve responder 401")
    void semTokenDeveResponder401() throws Exception {
        mockMvc.perform(get(ROTA)).andExpect(status().isUnauthorized());
    }
}
