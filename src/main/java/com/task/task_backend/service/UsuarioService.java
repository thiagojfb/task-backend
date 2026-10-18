package com.task.task_backend.service;

import com.task.task_backend.dto.CadastroUsuarioRequisicaoDTO;
import com.task.task_backend.dto.UsuarioCadastradoRespostaDTO;
import com.task.task_backend.seguranca.KeycloakAdminCliente;
import com.task.task_backend.seguranca.NovoUsuarioKeycloak;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Cadastro de contas de usuário delegado ao Keycloak (a API não guarda credenciais). */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final KeycloakAdminCliente keycloakAdminCliente;

    public UsuarioCadastradoRespostaDTO cadastrar(CadastroUsuarioRequisicaoDTO requisicao) {
        keycloakAdminCliente.criarUsuario(new NovoUsuarioKeycloak(
                requisicao.usuario(), requisicao.email(), requisicao.nome(), requisicao.sobrenome(),
                requisicao.senha()));
        log.info("Usuário {} cadastrado no provedor de identidade", requisicao.usuario());
        return new UsuarioCadastradoRespostaDTO(requisicao.usuario(), requisicao.email(),
                requisicao.nome() + " " + requisicao.sobrenome());
    }
}
