package com.task.task_backend.integration;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Base dos testes de integração: sobe um Keycloak efêmero (Testcontainers) com o mesmo realm usado no
 * Docker Compose e aponta o Resource Server para ele. O container é compartilhado por toda a JVM de testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class KeycloakContainerSuporte {

    protected static final String REALM = "tarefas";
    protected static final String CLIENTE_TESTE = "tarefas-teste";
    protected static final String ANA = "ana";
    protected static final String BRUNO = "bruno";
    protected static final String VISITANTE = "visitante";
    protected static final String SEMNOME = "semnome";
    protected static final String SENHA = "senha123";

    private static final KeycloakContainer KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:26.7.4")
            .withRealmImportFile("/keycloak/realm-export.json");

    static {
        KEYCLOAK.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @DynamicPropertySource
    static void configurarEmissor(DynamicPropertyRegistry registro) {
        String emissor = KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM;
        registro.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> emissor);
        registro.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> emissor + "/protocol/openid-connect/certs");
        registro.add("keycloak.admin.url", KEYCLOAK::getAuthServerUrl);
    }

    /** Obtém um token de acesso por login e senha (password grant) no cliente de testes. */
    protected static String obterToken(String usuario, String senha) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("grant_type", "password");
        formulario.add("client_id", CLIENTE_TESTE);
        formulario.add("username", usuario);
        formulario.add("password", senha);

        @SuppressWarnings("unchecked")
        Map<String, Object> resposta = RestClient.create()
                .post()
                .uri(KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(Map.class);
        return String.valueOf(resposta.get("access_token"));
    }

    /** Valor pronto para o cabeçalho Authorization. */
    protected static String bearer(String usuario) {
        return "Bearer " + obterToken(usuario, SENHA);
    }
}
