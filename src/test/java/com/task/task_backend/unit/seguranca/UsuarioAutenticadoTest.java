package com.task.task_backend.unit.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.task.task_backend.seguranca.UsuarioAutenticado;
import com.task.task_backend.seguranca.UsuarioLogado;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("UsuarioAutenticado")
class UsuarioAutenticadoTest {

    private final UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve usar o claim name como nome de exibição")
    void deveUsarClaimNameComoNome() {
        autenticarCom(Map.of("sub", "ana-id", "name", "Ana Silva", "preferred_username", "ana"));

        UsuarioLogado usuario = usuarioAutenticado.obter();

        assertThat(usuario.id()).isEqualTo("ana-id");
        assertThat(usuario.nome()).isEqualTo("Ana Silva");
    }

    @Test
    @DisplayName("Deve usar preferred_username quando name estiver ausente")
    void deveUsarPreferredUsernameQuandoNameAusente() {
        autenticarCom(Map.of("sub", "semnome-id", "preferred_username", "semnome"));

        assertThat(usuarioAutenticado.obter().nome()).isEqualTo("semnome");
    }

    @Test
    @DisplayName("Deve usar o identificador quando não houver nome nem usuário")
    void deveUsarIdentificadorQuandoNaoHouverNome() {
        autenticarCom(Map.of("sub", "somente-id"));

        assertThat(usuarioAutenticado.obter().nome()).isEqualTo("somente-id");
    }

    @Test
    @DisplayName("Deve lançar exceção quando não houver autenticação no contexto")
    void deveLancarExcecaoSemAutenticacao() {
        assertThatThrownBy(usuarioAutenticado::obter)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Nenhum usuário autenticado no contexto.");
    }

    @Test
    @DisplayName("Deve lançar exceção quando a autenticação não for baseada em JWT")
    void deveLancarExcecaoQuandoAutenticacaoNaoForJwt() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("ana", "senha", List.of()));

        assertThatThrownBy(usuarioAutenticado::obter)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Nenhum usuário autenticado no contexto.");
    }

    private void autenticarCom(Map<String, Object> claims) {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "RS256"), claims);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
