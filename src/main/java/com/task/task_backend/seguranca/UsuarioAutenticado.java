package com.task.task_backend.seguranca;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Lê o usuário autenticado a partir do JWT presente no contexto de segurança. */
@Component
public class UsuarioAutenticado {

    private static final String CLAIM_NOME = "name";
    private static final String CLAIM_USUARIO = "preferred_username";

    public UsuarioLogado obter() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacao instanceof JwtAuthenticationToken tokenJwt)) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto.");
        }
        Jwt jwt = tokenJwt.getToken();
        String id = jwt.getSubject();
        String nome = jwt.getClaimAsString(CLAIM_NOME);
        if (nome == null) {
            nome = jwt.getClaimAsString(CLAIM_USUARIO);
        }
        if (nome == null) {
            nome = id;
        }
        return new UsuarioLogado(id, nome);
    }
}
