package com.task.task_backend.exception;

import com.task.task_backend.dto.ErroValidacaoDTO;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Converte exceções em respostas {@link ProblemDetail} (RFC 9457) com mensagens em português. */
@Slf4j
@RestControllerAdvice
public class TratadorGlobalExcecoes extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TarefaNaoEncontradaException.class)
    public ProblemDetail tratarTarefaNaoEncontrada(TarefaNaoEncontradaException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excecao.getMessage());
        problema.setTitle("Tarefa não encontrada");
        return problema;
    }

    @ExceptionHandler(PosicaoInvalidaException.class)
    public ProblemDetail tratarPosicaoInvalida(PosicaoInvalidaException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, excecao.getMessage());
        problema.setTitle("Posição inválida");
        return problema;
    }

    @ExceptionHandler(UsuarioJaExisteException.class)
    public ProblemDetail tratarUsuarioJaExiste(UsuarioJaExisteException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excecao.getMessage());
        problema.setTitle("Usuário já cadastrado");
        return problema;
    }

    @ExceptionHandler(FalhaProvedorIdentidadeException.class)
    public ProblemDetail tratarFalhaProvedorIdentidade(FalhaProvedorIdentidadeException excecao) {
        log.error("Falha ao comunicar com o provedor de identidade", excecao);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, excecao.getMessage());
        problema.setTitle("Falha no provedor de identidade");
        return problema;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail tratarErroInesperado(Exception excecao) {
        log.error("Erro inesperado ao processar requisição", excecao);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        problema.setTitle("Erro interno");
        return problema;
    }

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException excecao,
            HttpHeaders cabecalhos, HttpStatusCode status, WebRequest requisicao) {
        List<ErroValidacaoDTO> erros = excecao.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(erro -> new ErroValidacaoDTO(erro.getField(), erro.getDefaultMessage()))
                .toList();
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos.");
        problema.setTitle("Dados inválidos");
        problema.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    public ResponseEntity<Object> handleTypeMismatch(TypeMismatchException excecao, HttpHeaders cabecalhos,
            HttpStatusCode status, WebRequest requisicao) {
        String parametro = excecao instanceof MethodArgumentTypeMismatchException argumento
                ? argumento.getName()
                : excecao.getPropertyName();
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "O valor informado para o parâmetro '%s' é inválido.".formatted(parametro));
        problema.setTitle("Parâmetro inválido");
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException excecao,
            HttpHeaders cabecalhos, HttpStatusCode status, WebRequest requisicao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "O corpo da requisição está malformado ou contém valores não reconhecidos.");
        problema.setTitle("Corpo da requisição inválido");
        return ResponseEntity.badRequest().body(problema);
    }
}
