package com.task.task_backend.unit.seguranca;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.task.task_backend.config.KeycloakAdminPropriedades;
import com.task.task_backend.exception.FalhaProvedorIdentidadeException;
import com.task.task_backend.exception.UsuarioJaExisteException;
import com.task.task_backend.seguranca.KeycloakAdminCliente;
import com.task.task_backend.seguranca.NovoUsuarioKeycloak;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("KeycloakAdminCliente")
class KeycloakAdminClienteTest {

    private static final String BASE = "http://keycloak:8080";
    private static final String TOKEN_URL = BASE + "/realms/tarefas/protocol/openid-connect/token";
    private static final String USUARIOS_URL = BASE + "/admin/realms/tarefas/users";
    private static final NovoUsuarioKeycloak NOVO =
            new NovoUsuarioKeycloak("carla", "carla@exemplo.com.br", "Carla", "Dias", "segredo123");

    private MockRestServiceServer servidor;
    private KeycloakAdminCliente cliente;

    @BeforeEach
    void configurar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new KeycloakAdminCliente(builder,
                new KeycloakAdminPropriedades(BASE, "tarefas", "tarefas-api", "segredo-api"));
    }

    @Test
    @DisplayName("Deve obter token de serviço e criar o usuário habilitado com senha definitiva")
    void deveObterTokenECriarUsuario() {
        esperarToken();
        servidor.expect(requestTo(USUARIOS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-servico"))
                .andExpect(jsonPath("$.username").value("carla"))
                .andExpect(jsonPath("$.email").value("carla@exemplo.com.br"))
                .andExpect(jsonPath("$.firstName").value("Carla"))
                .andExpect(jsonPath("$.lastName").value("Dias"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.credentials[0].type").value("password"))
                .andExpect(jsonPath("$.credentials[0].value").value("segredo123"))
                .andExpect(jsonPath("$.credentials[0].temporary").value(false))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertThatCode(() -> cliente.criarUsuario(NOVO)).doesNotThrowAnyException();
        servidor.verify();
    }

    @Test
    @DisplayName("Conflito no Keycloak deve virar UsuarioJaExisteException")
    void conflitoDeveVirarUsuarioJaExiste() {
        esperarToken();
        servidor.expect(requestTo(USUARIOS_URL)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> cliente.criarUsuario(NOVO))
                .isInstanceOf(UsuarioJaExisteException.class)
                .hasMessage("Já existe um usuário com este nome de usuário ou e-mail.");
    }

    @Test
    @DisplayName("Erro inesperado ao criar deve virar FalhaProvedorIdentidadeException")
    void erroAoCriarDeveVirarFalhaProvedor() {
        esperarToken();
        servidor.expect(requestTo(USUARIOS_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> cliente.criarUsuario(NOVO))
                .isInstanceOf(FalhaProvedorIdentidadeException.class)
                .hasMessage("Não foi possível cadastrar o usuário no provedor de identidade.");
    }

    @Test
    @DisplayName("Falha ao obter o token de serviço deve virar FalhaProvedorIdentidadeException")
    void falhaNoTokenDeveVirarFalhaProvedor() {
        servidor.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> cliente.criarUsuario(NOVO)).isInstanceOf(FalhaProvedorIdentidadeException.class);
    }

    @Test
    @DisplayName("Resposta de token sem access_token deve virar FalhaProvedorIdentidadeException")
    void tokenSemAccessTokenDeveVirarFalhaProvedor() {
        servidor.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.criarUsuario(NOVO)).isInstanceOf(FalhaProvedorIdentidadeException.class);
    }

    @Test
    @DisplayName("Resposta de token sem corpo deve virar FalhaProvedorIdentidadeException")
    void tokenSemCorpoDeveVirarFalhaProvedor() {
        servidor.expect(requestTo(TOKEN_URL)).andRespond(withSuccess());

        assertThatThrownBy(() -> cliente.criarUsuario(NOVO)).isInstanceOf(FalhaProvedorIdentidadeException.class);
    }

    private void esperarToken() {
        servidor.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(Matchers.containsString("grant_type=client_credentials")))
                .andExpect(content().string(Matchers.containsString("client_id=tarefas-api")))
                .andExpect(content().string(Matchers.containsString("client_secret=segredo-api")))
                .andRespond(withSuccess("{\"access_token\":\"token-servico\",\"expires_in\":60}",
                        MediaType.APPLICATION_JSON));
    }
}
