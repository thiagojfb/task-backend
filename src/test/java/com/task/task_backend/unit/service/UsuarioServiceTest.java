package com.task.task_backend.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.task.task_backend.dto.CadastroUsuarioRequisicaoDTO;
import com.task.task_backend.dto.UsuarioCadastradoRespostaDTO;
import com.task.task_backend.seguranca.KeycloakAdminCliente;
import com.task.task_backend.seguranca.NovoUsuarioKeycloak;
import com.task.task_backend.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService")
class UsuarioServiceTest {

    @Mock
    private KeycloakAdminCliente keycloakAdminCliente;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Deve enviar o novo usuário ao Keycloak e devolver o nome completo")
    void deveCadastrarUsuario() {
        CadastroUsuarioRequisicaoDTO requisicao =
                new CadastroUsuarioRequisicaoDTO("Carla", "Dias", "carla@exemplo.com.br", "carla", "segredo123");

        UsuarioCadastradoRespostaDTO resposta = usuarioService.cadastrar(requisicao);

        verify(keycloakAdminCliente).criarUsuario(
                new NovoUsuarioKeycloak("carla", "carla@exemplo.com.br", "Carla", "Dias", "segredo123"));
        assertThat(resposta.usuario()).isEqualTo("carla");
        assertThat(resposta.email()).isEqualTo("carla@exemplo.com.br");
        assertThat(resposta.nome()).isEqualTo("Carla Dias");
    }
}
