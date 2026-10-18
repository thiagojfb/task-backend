package com.task.task_backend.unit.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.task.task_backend.dto.ErroValidacaoDTO;
import com.task.task_backend.exception.FalhaProvedorIdentidadeException;
import com.task.task_backend.exception.PosicaoInvalidaException;
import com.task.task_backend.exception.TarefaNaoEncontradaException;
import com.task.task_backend.exception.TratadorGlobalExcecoes;
import com.task.task_backend.exception.UsuarioJaExisteException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@DisplayName("TratadorGlobalExcecoes")
class TratadorGlobalExcecoesTest {

    private final TratadorGlobalExcecoes tratador = new TratadorGlobalExcecoes();

    @Test
    @DisplayName("Tarefa não encontrada deve gerar 404 com título em português")
    void tarefaNaoEncontradaDeveGerar404() {
        ProblemDetail problema = tratador.tratarTarefaNaoEncontrada(new TarefaNaoEncontradaException());

        assertThat(problema.getStatus()).isEqualTo(404);
        assertThat(problema.getTitle()).isEqualTo("Tarefa não encontrada");
        assertThat(problema.getDetail()).isEqualTo("Não existe tarefa com o identificador informado.");
    }

    @Test
    @DisplayName("Posição inválida deve gerar 400 com o intervalo permitido")
    void posicaoInvalidaDeveGerar400() {
        ProblemDetail problema = tratador.tratarPosicaoInvalida(new PosicaoInvalidaException(1, 5));

        assertThat(problema.getStatus()).isEqualTo(400);
        assertThat(problema.getTitle()).isEqualTo("Posição inválida");
        assertThat(problema.getDetail()).isEqualTo("A posição deve estar entre 1 e 5.");
    }

    @Test
    @DisplayName("Usuário já existente deve gerar 409")
    void usuarioJaExistenteDeveGerar409() {
        ProblemDetail problema = tratador.tratarUsuarioJaExiste(new UsuarioJaExisteException());

        assertThat(problema.getStatus()).isEqualTo(409);
        assertThat(problema.getTitle()).isEqualTo("Usuário já cadastrado");
        assertThat(problema.getDetail()).isEqualTo("Já existe um usuário com este nome de usuário ou e-mail.");
    }

    @Test
    @DisplayName("Falha no provedor de identidade deve gerar 502 sem vazar a causa")
    void falhaNoProvedorDeveGerar502() {
        ProblemDetail problema = tratador.tratarFalhaProvedorIdentidade(
                new FalhaProvedorIdentidadeException(new RuntimeException("conexão recusada")));

        assertThat(problema.getStatus()).isEqualTo(502);
        assertThat(problema.getTitle()).isEqualTo("Falha no provedor de identidade");
        assertThat(problema.getDetail()).isEqualTo("Não foi possível cadastrar o usuário no provedor de identidade.");
    }

    @Test
    @DisplayName("Falha de validação deve listar os campos ordenados")
    void falhaDeValidacaoDeveListarCamposOrdenados() {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "tarefa");
        resultado.addError(new FieldError("tarefa", "valor", "O valor não pode ser negativo."));
        resultado.addError(new FieldError("tarefa", "descricao", "A descrição é obrigatória."));
        MethodArgumentNotValidException excecao =
                new MethodArgumentNotValidException(mock(MethodParameter.class), resultado);

        ResponseEntity<Object> resposta = tratador.handleMethodArgumentNotValid(
                excecao, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        ProblemDetail problema = (ProblemDetail) resposta.getBody();
        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(problema.getTitle()).isEqualTo("Dados inválidos");
        assertThat(problema.getDetail()).isEqualTo("Um ou mais campos estão inválidos.");
        @SuppressWarnings("unchecked")
        List<ErroValidacaoDTO> erros = (List<ErroValidacaoDTO>) problema.getProperties().get("erros");
        assertThat(erros).containsExactly(
                new ErroValidacaoDTO("descricao", "A descrição é obrigatória."),
                new ErroValidacaoDTO("valor", "O valor não pode ser negativo."));
    }

    @Test
    @DisplayName("Corpo ilegível deve gerar 400 com título específico")
    void corpoIlegivelDeveGerar400() {
        HttpMessageNotReadableException excecao =
                new HttpMessageNotReadableException("JSON inválido", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Object> resposta = tratador.handleHttpMessageNotReadable(
                excecao, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        ProblemDetail problema = (ProblemDetail) resposta.getBody();
        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(problema.getTitle()).isEqualTo("Corpo da requisição inválido");
        assertThat(problema.getDetail())
                .isEqualTo("O corpo da requisição está malformado ou contém valores não reconhecidos.");
    }

    @Test
    @DisplayName("Exceção genérica deve gerar 500 sem vazar a mensagem interna")
    void excecaoGenericaDeveGerar500SemVazarMensagem() {
        ProblemDetail problema = tratador.tratarErroInesperado(new RuntimeException("detalhe interno secreto"));

        assertThat(problema.getStatus()).isEqualTo(500);
        assertThat(problema.getTitle()).isEqualTo("Erro interno");
        assertThat(problema.getDetail()).isEqualTo("Ocorreu um erro inesperado. Tente novamente mais tarde.");
        assertThat(problema.getDetail()).doesNotContain("secreto");
    }

    @Test
    @DisplayName("Parâmetro de requisição com tipo inválido deve gerar 400 com o nome do parâmetro")
    void parametroComTipoInvalidoDeveGerar400() {
        MethodParameter parametro = mock(MethodParameter.class);
        MethodArgumentTypeMismatchException excecao =
                new MethodArgumentTypeMismatchException("ATRASADAS", Object.class, "situacao", parametro, null);

        ResponseEntity<Object> resposta = tratador.handleTypeMismatch(
                excecao, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        ProblemDetail problema = (ProblemDetail) resposta.getBody();
        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(problema.getTitle()).isEqualTo("Parâmetro inválido");
        assertThat(problema.getDetail()).isEqualTo("O valor informado para o parâmetro 'situacao' é inválido.");
    }

    @Test
    @DisplayName("Incompatibilidade de tipo genérica deve usar o nome da propriedade")
    void incompatibilidadeGenericaDeveUsarNomeDaPropriedade() {
        TypeMismatchException excecao = new TypeMismatchException("abc", Integer.class);
        excecao.initPropertyName("quantidade");

        ResponseEntity<Object> resposta = tratador.handleTypeMismatch(
                excecao, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        ProblemDetail problema = (ProblemDetail) resposta.getBody();
        assertThat(problema.getDetail()).isEqualTo("O valor informado para o parâmetro 'quantidade' é inválido.");
    }

    @Test
    @DisplayName("Validação sem erros de campo deve retornar lista vazia")
    void validacaoSemErrosDeCampoDeveRetornarListaVazia() {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "tarefa");
        MethodParameter parametro = mock(MethodParameter.class);
        when(parametro.getParameterIndex()).thenReturn(0);
        MethodArgumentNotValidException excecao = new MethodArgumentNotValidException(parametro, resultado);

        ResponseEntity<Object> resposta = tratador.handleMethodArgumentNotValid(
                excecao, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        ProblemDetail problema = (ProblemDetail) resposta.getBody();
        assertThat((List<?>) problema.getProperties().get("erros")).isEmpty();
    }
}
