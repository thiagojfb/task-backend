package com.task.task_backend.service;

import com.task.task_backend.dto.MoverTarefaRequisicaoDTO;
import com.task.task_backend.dto.RegistroHistoricoRespostaDTO;
import com.task.task_backend.dto.TarefaRequisicaoDTO;
import com.task.task_backend.dto.TarefaRespostaDTO;
import com.task.task_backend.entity.FiltroSituacao;
import com.task.task_backend.entity.Situacao;
import com.task.task_backend.entity.Tarefa;
import com.task.task_backend.exception.PosicaoInvalidaException;
import com.task.task_backend.exception.TarefaNaoEncontradaException;
import com.task.task_backend.mapper.RegistroHistoricoMapper;
import com.task.task_backend.mapper.TarefaMapper;
import com.task.task_backend.repository.RegistroHistoricoRepository;
import com.task.task_backend.repository.TarefaRepository;
import com.task.task_backend.seguranca.UsuarioAutenticado;
import com.task.task_backend.seguranca.UsuarioLogado;
import com.task.task_backend.util.NormalizadorTexto;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de negócio das tarefas; toda operação é restrita ao usuário autenticado. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final RegistroHistoricoRepository historicoRepository;
    private final TarefaMapper tarefaMapper;
    private final RegistroHistoricoMapper historicoMapper;
    private final HistoricoService historicoService;
    private final UsuarioAutenticado usuarioAutenticado;
    private final Clock relogio;

    @Transactional
    public TarefaRespostaDTO criar(TarefaRequisicaoDTO requisicao) {
        UsuarioLogado usuario = usuarioAutenticado.obter();
        Tarefa tarefa = tarefaMapper.paraEntidade(requisicao);
        tarefa.setId(UUID.randomUUID());
        tarefa.setUsuarioId(usuario.id());
        tarefa.setSituacao(Situacao.ABERTA);
        tarefa.setDataAbertura(LocalDateTime.now(relogio));
        tarefa.setPosicao(tarefaRepository.countByUsuarioId(usuario.id()) + 1);

        Tarefa salva = tarefaRepository.save(tarefa);
        historicoService.registrarCriacao(salva, usuario);
        log.info("Tarefa {} criada pelo usuário {}", salva.getId(), usuario.id());
        return tarefaMapper.paraResposta(salva, hoje());
    }

    /** Lista completa (sem paginação) por posição, com filtro de situação e busca normalizada opcionais. */
    @Transactional(readOnly = true)
    public List<TarefaRespostaDTO> listar(FiltroSituacao filtro, String busca) {
        String usuarioId = usuarioAutenticado.obter().id();
        Optional<Situacao> situacao = filtro.paraSituacao();
        String termo = NormalizadorTexto.normalizar(busca);

        List<Tarefa> tarefas;
        if (termo.isEmpty()) {
            tarefas = situacao
                    .map(s -> tarefaRepository.findByUsuarioIdAndSituacaoOrderByPosicaoAsc(usuarioId, s))
                    .orElseGet(() -> tarefaRepository.findByUsuarioIdOrderByPosicaoAsc(usuarioId));
        } else {
            tarefas = situacao
                    .map(s -> tarefaRepository.buscarPorSituacao(usuarioId, s, termo))
                    .orElseGet(() -> tarefaRepository.buscar(usuarioId, termo));
        }

        LocalDate hoje = hoje();
        return tarefas.stream()
                .map(tarefa -> tarefaMapper.paraResposta(tarefa, hoje))
                .toList();
    }

    @Transactional(readOnly = true)
    public TarefaRespostaDTO obter(UUID id) {
        return tarefaMapper.paraResposta(buscarDoUsuario(id, usuarioAutenticado.obter()), hoje());
    }

    @Transactional
    public TarefaRespostaDTO editar(UUID id, TarefaRequisicaoDTO requisicao) {
        UsuarioLogado usuario = usuarioAutenticado.obter();
        Tarefa tarefa = buscarDoUsuario(id, usuario);
        Tarefa anterior = tarefa.toBuilder().build();

        tarefa.setDescricao(requisicao.descricao());
        tarefa.setPrioridade(requisicao.prioridade());
        tarefa.setDataPrevistaConclusao(requisicao.dataPrevistaConclusao());
        tarefa.setValor(tarefaMapper.normalizarValor(requisicao.valor()));

        Tarefa salva = tarefaRepository.save(tarefa);
        historicoService.registrarAlteracoes(anterior, salva, usuario);
        log.info("Tarefa {} editada pelo usuário {}", salva.getId(), usuario.id());
        return tarefaMapper.paraResposta(salva, hoje());
    }

    @Transactional
    public TarefaRespostaDTO concluir(UUID id) {
        UsuarioLogado usuario = usuarioAutenticado.obter();
        Tarefa tarefa = buscarDoUsuario(id, usuario);
        if (tarefa.getSituacao() != Situacao.CONCLUIDA) {
            tarefa.setSituacao(Situacao.CONCLUIDA);
            tarefa.setDataConclusaoReal(LocalDateTime.now(relogio));
            // Tarefa concluída vai automaticamente para o final da lista (mudança de posição não gera histórico)
            deslocarPara(tarefa, tarefaRepository.countByUsuarioId(usuario.id()), usuario);
            tarefa = tarefaRepository.save(tarefa);
            historicoService.registrarMudancaSituacao(tarefa, Situacao.ABERTA, Situacao.CONCLUIDA, usuario);
            log.info("Tarefa {} concluída pelo usuário {}", tarefa.getId(), usuario.id());
        }
        return tarefaMapper.paraResposta(tarefa, hoje());
    }

    @Transactional
    public TarefaRespostaDTO reabrir(UUID id) {
        UsuarioLogado usuario = usuarioAutenticado.obter();
        Tarefa tarefa = buscarDoUsuario(id, usuario);
        if (tarefa.getSituacao() != Situacao.ABERTA) {
            tarefa.setSituacao(Situacao.ABERTA);
            tarefa.setDataConclusaoReal(null);
            tarefa = tarefaRepository.save(tarefa);
            historicoService.registrarMudancaSituacao(tarefa, Situacao.CONCLUIDA, Situacao.ABERTA, usuario);
            log.info("Tarefa {} reaberta pelo usuário {}", tarefa.getId(), usuario.id());
        }
        return tarefaMapper.paraResposta(tarefa, hoje());
    }

    @Transactional(readOnly = true)
    public List<RegistroHistoricoRespostaDTO> listarHistorico(UUID id) {
        Tarefa tarefa = buscarDoUsuario(id, usuarioAutenticado.obter());
        return historicoRepository.findByTarefaIdOrderByDataHoraDesc(tarefa.getId()).stream()
                .map(historicoMapper::paraResposta)
                .toList();
    }

    /** Move a tarefa para o topo, final ou posição específica mantendo a sequência contínua 1..N. */
    @Transactional
    public TarefaRespostaDTO mover(UUID id, MoverTarefaRequisicaoDTO requisicao) {
        UsuarioLogado usuario = usuarioAutenticado.obter();
        Tarefa tarefa = tarefaRepository.buscarParaMover(id, usuario.id())
                .orElseThrow(TarefaNaoEncontradaException::new);
        int total = tarefaRepository.countByUsuarioId(usuario.id());
        int origem = tarefa.getPosicao();
        int destino = switch (requisicao.destino()) {
            case TOPO -> 1;
            case FINAL -> total;
            case POSICAO -> validarPosicao(requisicao.posicao(), total);
        };

        if (deslocarPara(tarefa, destino, usuario)) {
            tarefa = tarefaRepository.save(tarefa);
            log.info("Tarefa {} movida da posição {} para {} pelo usuário {}", tarefa.getId(), origem, destino,
                    usuario.id());
        }
        return tarefaMapper.paraResposta(tarefa, hoje());
    }

    /**
     * Desloca as demais tarefas do usuário e ajusta a posição da tarefa em memória (sem salvar).
     * Devolve false quando a tarefa já está na posição de destino.
     */
    private boolean deslocarPara(Tarefa tarefa, int destino, UsuarioLogado usuario) {
        int origem = tarefa.getPosicao();
        if (destino == origem) {
            return false;
        }
        if (destino < origem) {
            tarefaRepository.incrementarPosicoes(usuario.id(), destino, origem - 1);
        } else {
            tarefaRepository.decrementarPosicoes(usuario.id(), origem + 1, destino);
        }
        tarefa.setPosicao(destino);
        return true;
    }

    /** Exclui definitivamente a tarefa (o histórico é removido em cascata pelo banco) e fecha a lacuna. */
    @Transactional
    public void excluir(UUID id) {
        UsuarioLogado usuario = usuarioAutenticado.obter();
        Tarefa tarefa = buscarDoUsuario(id, usuario);
        int posicao = tarefa.getPosicao();
        tarefaRepository.delete(tarefa);
        tarefaRepository.decrementarPosicoesApos(usuario.id(), posicao);
        log.info("Tarefa {} excluída pelo usuário {}", id, usuario.id());
    }

    private static int validarPosicao(Integer posicao, int total) {
        if (posicao == null || posicao < 1 || posicao > total) {
            throw new PosicaoInvalidaException(1, total);
        }
        return posicao;
    }

    private Tarefa buscarDoUsuario(UUID id, UsuarioLogado usuario) {
        return tarefaRepository.findByIdAndUsuarioId(id, usuario.id())
                .orElseThrow(TarefaNaoEncontradaException::new);
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }
}
