package com.task.task_backend.seguranca;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Converte as roles de realm do Keycloak ({@code realm_access.roles}) em autoridades {@code ROLE_*}. */
@Component
public class ConversorAutoridadesKeycloak implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CHAVE_ROLES = "roles";
    private static final String PREFIXO = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(CLAIM_REALM_ACCESS);
        if (realmAccess == null || !(realmAccess.get(CHAVE_ROLES) instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(String::valueOf)
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(PREFIXO + role))
                .toList();
    }
}
