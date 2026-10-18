package com.task.task_backend.service;

import com.task.task_backend.entity.RegistroHistorico;
import com.task.task_backend.entity.Situacao;
import com.task.task_backend.entity.Tarefa;
import com.task.task_backend.entity.TipoRegistro;
import com.task.task_backend.repository.RegistroHistoricoRepository;
import com.task.task_backend.seguranca.UsuarioLogado;
import com.task.task_backend.util.FormatadorHistorico;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Gera os registros automáticos de histórico das tarefas (um por campo de conteúdo alterado). */
@Service
@RequiredArgsConstructor
public class HistoricoService {

    public static final String CAMPO_DESCRICAO = "descricao";
    public static final String CAMPO_PRIORIDADE = "prioridade";
    public static final String CAMPO_DATA_PREVISTA = "dataPrevistaConclusao";
    public static final String CAMPO_VALOR = "valor";
    public static final String CAMPO_SITUACAO = "situacao";

    private final RegistroHistoricoRepository historicoRepository;
    private final Clock relogio;

    public void registrarCriacao(Tarefa tarefa, UsuarioLogado usuario) {
        historicoRepository.save(novoRegistro(tarefa, usuario, TipoRegistro.CRIACAO, null, null, null));
    }

    /** Compara o estado anterior com o atual e grava um registro ALTERACAO por campo modificado. */
    public void registrarAlteracoes(Tarefa anterior, Tarefa atual, UsuarioLogado usuario) {
        if (!Objects.equals(anterior.getDescricao(), atual.getDescricao())) {
            registrarAlteracao(atual, usuario, CAMPO_DESCRICAO, anterior.getDescricao(), atual.getDescricao());
        }
        if (anterior.getPrioridade() != atual.getPrioridade()) {
            registrarAlteracao(atual, usuario, CAMPO_PRIORIDADE,
                    FormatadorHistorico.formatarPrioridade(anterior.getPrioridade()),
                    FormatadorHistorico.formatarPrioridade(atual.getPrioridade()));
        }
        if (!Objects.equals(anterior.getDataPrevistaConclusao(), atual.getDataPrevistaConclusao())) {
            registrarAlteracao(atual, usuario, CAMPO_DATA_PREVISTA,
                    FormatadorHistorico.formatarData(anterior.getDataPrevistaConclusao()),
                    FormatadorHistorico.formatarData(atual.getDataPrevistaConclusao()));
        }
        if (valorMudou(anterior.getValor(), atual.getValor())) {
            registrarAlteracao(atual, usuario, CAMPO_VALOR,
                    FormatadorHistorico.formatarValor(anterior.getValor()),
                    FormatadorHistorico.formatarValor(atual.getValor()));
        }
    }

    /** Registra conclusão ou reabertura como mudança do campo situação. */
    public void registrarMudancaSituacao(Tarefa tarefa, Situacao de, Situacao para, UsuarioLogado usuario) {
        TipoRegistro tipo = para == Situacao.CONCLUIDA ? TipoRegistro.CONCLUSAO : TipoRegistro.REABERTURA;
        historicoRepository.save(novoRegistro(tarefa, usuario, tipo, CAMPO_SITUACAO,
                FormatadorHistorico.formatarSituacao(de), FormatadorHistorico.formatarSituacao(para)));
    }

    private static boolean valorMudou(BigDecimal anterior, BigDecimal atual) {
        return anterior.compareTo(atual) != 0;
    }

    private void registrarAlteracao(Tarefa tarefa, UsuarioLogado usuario, String campo,
            String valorAnterior, String valorNovo) {
        historicoRepository.save(
                novoRegistro(tarefa, usuario, TipoRegistro.ALTERACAO, campo, valorAnterior, valorNovo));
    }

    private RegistroHistorico novoRegistro(Tarefa tarefa, UsuarioLogado usuario, TipoRegistro tipo,
            String campo, String valorAnterior, String valorNovo) {
        return RegistroHistorico.builder()
                .id(UUID.randomUUID())
                .tarefa(tarefa)
                .usuarioId(usuario.id())
                .usuarioNome(usuario.nome())
                .dataHora(LocalDateTime.now(relogio))
                .tipo(tipo)
                .campo(campo)
                .valorAnterior(valorAnterior)
                .valorNovo(valorNovo)
                .build();
    }
}
