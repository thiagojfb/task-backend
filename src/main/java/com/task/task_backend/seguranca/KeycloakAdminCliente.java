package com.task.task_backend.seguranca;

import com.task.task_backend.config.KeycloakAdminPropriedades;
import com.task.task_backend.exception.FalhaProvedorIdentidadeException;
import com.task.task_backend.exception.UsuarioJaExisteException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cria usuários no Keycloak pela Admin REST API autenticando-se como service account. */
@Component
public class KeycloakAdminCliente {

    private static final String ROTA_TOKEN = "/realms/{realm}/protocol/openid-connect/token";
    private static final String ROTA_USUARIOS = "/admin/realms/{realm}/users";

    private final RestClient restClient;
    private final KeycloakAdminPropriedades propriedades;

    public KeycloakAdminCliente(RestClient.Builder builder, KeycloakAdminPropriedades propriedades) {
        this.restClient = builder.baseUrl(propriedades.url()).build();
        this.propriedades = propriedades;
    }

    /** Cria o usuário habilitado, com e-mail verificado e senha definitiva. */
    public void criarUsuario(NovoUsuarioKeycloak novo) {
        String token = obterTokenServico();
        Map<String, Object> corpo = Map.of(
                "username", novo.username(),
                "email", novo.email(),
                "firstName", novo.firstName(),
                "lastName", novo.lastName(),
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of("type", "password", "value", novo.senha(), "temporary", false)));
        try {
            restClient.post()
                    .uri(ROTA_USUARIOS, propriedades.realm())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict excecao) {
            throw new UsuarioJaExisteException();
        } catch (RestClientException excecao) {
            throw new FalhaProvedorIdentidadeException(excecao);
        }
    }

    private String obterTokenServico() {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("grant_type", "client_credentials");
        formulario.add("client_id", propriedades.clientId());
        formulario.add("client_secret", propriedades.clientSecret());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resposta = restClient.post()
                    .uri(ROTA_TOKEN, propriedades.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(Map.class);
            if (resposta == null || !(resposta.get("access_token") instanceof String token)) {
                throw new FalhaProvedorIdentidadeException(
                        new IllegalStateException("Resposta do token de serviço sem access_token"));
            }
            return token;
        } catch (RestClientException excecao) {
            throw new FalhaProvedorIdentidadeException(excecao);
        }
    }
}
