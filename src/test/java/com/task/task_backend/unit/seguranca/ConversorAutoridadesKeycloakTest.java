package com.task.task_backend.unit.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import com.task.task_backend.seguranca.ConversorAutoridadesKeycloak;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("ConversorAutoridadesKeycloak")
class ConversorAutoridadesKeycloakTest {

    private final ConversorAutoridadesKeycloak conversor = new ConversorAutoridadesKeycloak();

    @Test
    @DisplayName("Deve converter roles do realm em autoridades com prefixo ROLE_")
    void deveConverterRolesDoRealmEmAutoridades() {
        Jwt jwt = jwtCom(Map.of("realm_access", Map.of("roles", List.of("usuario", "outra"))));

        Collection<GrantedAuthority> autoridades = conversor.convert(jwt);

        assertThat(autoridades)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_usuario", "ROLE_outra");
    }

    @Test
    @DisplayName("Deve retornar vazio quando o claim realm_access estiver ausente")
    void deveRetornarVazioSemRealmAccess() {
        assertThat(conversor.convert(jwtCom(Map.of("sub", "x")))).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar vazio quando realm_access não tiver roles")
    void deveRetornarVazioSemRoles() {
        assertThat(conversor.convert(jwtCom(Map.of("realm_access", Map.of("outro", "valor"))))).isEmpty();
    }

    private static Jwt jwtCom(Map<String, Object> claims) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "RS256"), claims);
    }
}
