package com.task.task_backend.unit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.task.task_backend.config.SegurancaConfig;
import com.task.task_backend.controller.PreferenciaController;
import com.task.task_backend.dto.PreferenciasDTO;
import com.task.task_backend.entity.FiltroSituacao;
import com.task.task_backend.exception.TratadorGlobalExcecoes;
import com.task.task_backend.seguranca.ConversorAutoridadesKeycloak;
import com.task.task_backend.service.PreferenciaService;
import org.junit.jupiter.api.DisplayName;
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

@WebMvcTest(PreferenciaController.class)
@Import({SegurancaConfig.class, TratadorGlobalExcecoes.class, ConversorAutoridadesKeycloak.class})
@ActiveProfiles("test")
@DisplayName("PreferenciaController")
class PreferenciaControllerTest {

    private static final String ROTA = "/api/v1/preferencias";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreferenciaService preferenciaService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static JwtRequestPostProcessor comoAna() {
        return jwt().jwt(jwt -> jwt.subject("ana-id")).authorities(new SimpleGrantedAuthority("ROLE_usuario"));
    }

    @Test
    @DisplayName("GET deve devolver a preferência do usuário")
    void getDeveDevolverPreferencia() throws Exception {
        when(preferenciaService.obter()).thenReturn(new PreferenciasDTO(FiltroSituacao.ABERTAS));

        mockMvc.perform(get(ROTA).with(comoAna()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filtro").value("ABERTAS"));
    }

    @Test
    @DisplayName("PUT deve salvar e devolver a preferência")
    void putDeveSalvarPreferencia() throws Exception {
        when(preferenciaService.salvar(any())).thenReturn(new PreferenciasDTO(FiltroSituacao.CONCLUIDAS));

        mockMvc.perform(put(ROTA).with(comoAna()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filtro":"CONCLUIDAS"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filtro").value("CONCLUIDAS"));
    }

    @Test
    @DisplayName("PUT sem filtro deve responder 400 em português")
    void putSemFiltroDeveResponder400() throws Exception {
        mockMvc.perform(put(ROTA).with(comoAna()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros[0].campo").value("filtro"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("O filtro é obrigatório."));
    }

    @Test
    @DisplayName("PUT com filtro desconhecido deve responder 400 Corpo inválido")
    void putComFiltroDesconhecidoDeveResponder400() throws Exception {
        mockMvc.perform(put(ROTA).with(comoAna()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filtro":"ATRASADAS"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
    }

    @Test
    @DisplayName("Sem token deve responder 401")
    void semTokenDeveResponder401() throws Exception {
        mockMvc.perform(get(ROTA)).andExpect(status().isUnauthorized());
    }
}
