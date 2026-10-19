package com.task.task_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentação OpenAPI em português com esquema de segurança Bearer JWT (Keycloak). */
@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_BEARER = "bearerAuth";
    public static final String TAG_TAREFAS = "Tarefas";
    public static final String TAG_HISTORICO = "Histórico";
    public static final String TAG_FINANCEIRO = "Financeiro";
    public static final String TAG_USUARIOS = "Usuários";
    public static final String TAG_PREFERENCIAS = "Preferências";

    @Bean
    public OpenAPI documentacaoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gerenciamento de Tarefas")
                        .version("1.0.0")
                        .description("""
                                Lista de tarefas pessoais com prioridade, controle financeiro (valor em reais), \
                                reordenação manual, histórico de alterações e autenticação via Keycloak. \
                                Todas as operações exigem token JWT (Bearer) emitido pelo realm `tarefas`; \
                                cada usuário acessa somente as próprias tarefas."""))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("""
                                Token de acesso emitido pelo Keycloak (realm `tarefas`). Em desenvolvimento, obtenha \
                                com login e senha via cliente `tarefas-teste`: \
                                POST http://localhost:8180/realms/tarefas/protocol/openid-connect/token \
                                (grant_type=password&client_id=tarefas-teste&username=ana&password=senha123).""")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER))
                .tags(List.of(
                        new Tag().name(TAG_TAREFAS)
                                .description("Cadastro, consulta, edição, conclusão, reordenação e exclusão de tarefas"),
                        new Tag().name(TAG_HISTORICO).description("Linha do tempo de alterações de uma tarefa"),
                        new Tag().name(TAG_FINANCEIRO).description("Indicadores financeiros"),
                        new Tag().name(TAG_USUARIOS)
                                .description("Criação de contas no provedor de identidade (Keycloak)"),
                        new Tag().name(TAG_PREFERENCIAS)
                                .description("Preferências de interface persistidas por usuário")));
    }
}
