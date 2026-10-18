package com.task.task_backend.config;

import com.task.task_backend.seguranca.ConversorAutoridadesKeycloak;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * API stateless protegida por JWT do Keycloak. Somente Swagger, contrato OpenAPI e health são públicos;
 * tudo em /api exige a role "usuario"; qualquer outra rota é negada.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SegurancaConfig {

    public static final String ROLE_USUARIO = "usuario";

    private final ConversorAutoridadesKeycloak conversorAutoridades;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain cadeiaDeFiltros(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter conversorJwt = new JwtAuthenticationConverter();
        conversorJwt.setJwtGrantedAuthoritiesConverter(conversorAutoridades);

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Console H2 usa frames; só é registrado no perfil dev (spring.h2.console.enabled)
                .headers(cabecalhos -> cabecalhos.frameOptions(frames -> frames.sameOrigin()))
                .authorizeHttpRequests(autorizacao -> autorizacao
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/actuator/health", "/h2-console/**").permitAll()
                        // Cadastro de conta: única rota pública de /api (Princípio IV)
                        .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").permitAll()
                        .requestMatchers("/api/**").hasRole(ROLE_USUARIO)
                        .anyRequest().denyAll())
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(pontoDeEntradaNaoAutenticado())
                        .accessDeniedHandler(tratadorAcessoNegado()))
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorJwt))
                        .authenticationEntryPoint(pontoDeEntradaNaoAutenticado())
                        .accessDeniedHandler(tratadorAcessoNegado()));
        return http.build();
    }

    private AuthenticationEntryPoint pontoDeEntradaNaoAutenticado() {
        return (requisicao, resposta, excecao) -> escreverProblema(resposta, HttpStatus.UNAUTHORIZED,
                "Não autenticado", "É necessário um token de acesso válido.");
    }

    private AccessDeniedHandler tratadorAcessoNegado() {
        return (requisicao, resposta, excecao) -> escreverProblema(resposta, HttpStatus.FORBIDDEN,
                "Acesso negado", "Você não tem permissão para acessar este recurso.");
    }

    private void escreverProblema(HttpServletResponse resposta, HttpStatus status, String titulo, String detalhe)
            throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write(objectMapper.writeValueAsString(problema));
    }
}
