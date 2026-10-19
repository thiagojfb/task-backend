package com.task.task_backend.unit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.task.task_backend.config.SegurancaConfig;
import com.task.task_backend.controller.UsuarioController;
import com.task.task_backend.dto.UsuarioCadastradoRespostaDTO;
import com.task.task_backend.exception.FalhaProvedorIdentidadeException;
import com.task.task_backend.exception.TratadorGlobalExcecoes;
import com.task.task_backend.exception.UsuarioJaExisteException;
import com.task.task_backend.seguranca.ConversorAutoridadesKeycloak;
import com.task.task_backend.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UsuarioController.class)
@Import({SegurancaConfig.class, TratadorGlobalExcecoes.class, ConversorAutoridadesKeycloak.class})
@ActiveProfiles("test")
@DisplayName("UsuarioController")
class UsuarioControllerTest {

    private static final String ROTA = "/api/v1/usuarios";
    private static final String CORPO_VALIDO = """
            {"nome":"Carla","sobrenome":"Dias","email":"carla@exemplo.com.br","usuario":"carla","senha":"segredo123"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Deve cadastrar sem token e retornar 201")
    void deveCadastrarSemTokenERetornar201() throws Exception {
        when(usuarioService.cadastrar(any()))
                .thenReturn(new UsuarioCadastradoRespostaDTO("carla", "carla@exemplo.com.br", "Carla Dias"));

        mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value("carla"))
                .andExpect(jsonPath("$.email").value("carla@exemplo.com.br"))
                .andExpect(jsonPath("$.nome").value("Carla Dias"));
    }

    @Test
    @DisplayName("Deve rejeitar dados inválidos com mensagens em português")
    void deveRejeitarDadosInvalidos() throws Exception {
        mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"","sobrenome":"  ","email":"nao-e-email","usuario":"a b","senha":"curta"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')].mensagem").value("O nome é obrigatório."))
                .andExpect(jsonPath("$.erros[?(@.campo=='sobrenome')].mensagem").value("O sobrenome é obrigatório."))
                .andExpect(jsonPath("$.erros[?(@.campo=='email')].mensagem").value("O e-mail é inválido."))
                .andExpect(jsonPath("$.erros[?(@.campo=='usuario')].mensagem").value(
                        "O nome de usuário deve ter de 3 a 50 caracteres (letras, números, ponto, hífen ou sublinhado)."))
                .andExpect(jsonPath("$.erros[?(@.campo=='senha')].mensagem").value("A senha deve ter entre 8 e 128 caracteres."));
    }

    @Test
    @DisplayName("Deve rejeitar e-mail em branco como obrigatório")
    void deveRejeitarEmailEmBranco() throws Exception {
        mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Carla","sobrenome":"Dias","email":"","usuario":"carla","senha":"segredo123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='email')].mensagem").value("O e-mail é obrigatório."));
    }

    @Test
    @DisplayName("Usuário duplicado deve retornar 409")
    void usuarioDuplicadoDeveRetornar409() throws Exception {
        when(usuarioService.cadastrar(any())).thenThrow(new UsuarioJaExisteException());

        mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Usuário já cadastrado"))
                .andExpect(jsonPath("$.detail").value("Já existe um usuário com este nome de usuário ou e-mail."));
    }

    @Test
    @DisplayName("Falha no provedor de identidade deve retornar 502")
    void falhaNoProvedorDeveRetornar502() throws Exception {
        when(usuarioService.cadastrar(any())).thenThrow(new FalhaProvedorIdentidadeException(new RuntimeException("x")));

        mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Falha no provedor de identidade"))
                .andExpect(jsonPath("$.detail").value("Não foi possível cadastrar o usuário no provedor de identidade."));
    }
}
