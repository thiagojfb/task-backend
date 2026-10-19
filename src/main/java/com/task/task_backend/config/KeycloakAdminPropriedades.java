package com.task.task_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Acesso à Admin REST API do Keycloak com um cliente confidencial de service account (segredo via ambiente). */
@ConfigurationProperties(prefix = "keycloak.admin")
public record KeycloakAdminPropriedades(String url, String realm, String clientId, String clientSecret) {
}
