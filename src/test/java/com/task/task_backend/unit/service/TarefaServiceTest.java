package com.task.task_backend.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.task.task_backend.dto.MoverTarefaRequisicaoDTO;
import com.task.task_backend.dto.RegistroHistoricoRespostaDTO;
import com.task.task_backend.dto.TarefaRequisicaoDTO;
import com.task.task_backend.dto.TarefaRespostaDTO;
import com.task.task_backend.entity.DestinoMovimento;
import com.task.task_backend.entity.FiltroSituacao;
import com.task.task_backend.entity.Prioridade;
import com.task.task_backend.entity.RegistroHistorico;
import com.task.task_backend.entity.Situacao;
import com.task.task_backend.entity.Tarefa;
import com.task.task_backend.entity.TipoRegistro;
import com.task.task_backend.exception.PosicaoInvalidaException;
import com.task.task_backend.exception.TarefaNaoEncontradaException;
import com.task.task_backend.mapper.RegistroHistoricoMapper;
import com.task.task_backend.mapper.TarefaMapper;
import com.task.task_backend.repository.RegistroHistoricoRepository;
import com.task.task_backend.repository.TarefaRepository;
import com.task.task_backend.seguranca.UsuarioAutenticado;
import com.task.task_backend.seguranca.UsuarioLogado;
import com.task.task_backend.service.HistoricoService;
import com.task.task_backend.service.TarefaService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TarefaService")
class TarefaServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 19, 14, 30, 0);
    private static final LocalDate HOJE = AGORA.toLocalDate();
    private static final UsuarioLogado ANA = new UsuarioLogado("ana-id", "Ana Silva");

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private HistoricoService historicoService;

    @Mock
    private UsuarioAutenticado usuarioAutenticado;

    @Mock
    private RegistroHistoricoRepository historicoRepository;

    private final TarefaMapper tarefaMapper = Mappers.getMapper(TarefaMapper.class);
    private final RegistroHistoricoMapper historicoMapper = Mappers.getMapper(RegistroHistoricoMapper.class);

    private TarefaService tarefaService;

    @BeforeEach
    void configurar() {
        Clock relogio = Clock.fixed(AGORA.atZone(FUSO).toInstant(), FUSO);
        tarefaService = new TarefaService(tarefaRepository, historicoRepository, tarefaMapper, historicoMapper,
                historicoService, usuarioAutenticado, relogio);
        when(usuarioAutenticado.obter()).thenReturn(ANA);
    }

    @Nested
    @DisplayName("listarHistorico")
    class ListarHistorico {

        @Test
        @DisplayName("Deve listar o histórico da tarefa do usuário na ordem retornada pelo repositório")
        void deveListarHistoricoDaTarefa() {
            Tarefa tarefa = tarefaBase().build();
            when(tarefaRepository.findByIdAndUsuarioId(tarefa.getId(), "ana-id")).thenReturn(Optional.of(tarefa));
            RegistroHistorico alteracao = RegistroHistorico.builder()
                    .id(UUID.randomUUID()).tarefa(tarefa).usuarioId("ana-id").usuarioNome("Ana Silva")
                    .dataHora(AGORA).tipo(TipoRegistro.ALTERACAO).campo("valor")
                    .valorAnterior("R$ 50,00").valorNovo("R$ 70,00").build();
            RegistroHistorico criacao = RegistroHistorico.builder()
                    .id(UUID.randomUUID()).tarefa(tarefa).usuarioId("ana-id").usuarioNome("Ana Silva")
                    .dataHora(AGORA.minusHours(1)).tipo(TipoRegistro.CRIACAO).build();
            when(historicoRepository.findByTarefaIdOrderByDataHoraDesc(tarefa.getId()))
                    .thenReturn(List.of(alteracao, criacao));

            List<RegistroHistoricoRespostaDTO> historico = tarefaService.listarHistorico(tarefa.getId());

            assertThat(historico).extracting(RegistroHistoricoRespostaDTO::tipo)
                    .containsExactly(TipoRegistro.ALTERACAO, TipoRegistro.CRIACAO);
            assertThat(historico.get(0).descricao())
                    .isEqualTo("Modificado por Ana Silva em 19/09/2026 - Alterou valor de R$ 50,00 para R$ 70,00");
        }

        @Test
        @DisplayName("Deve lançar exceção ao consultar histórico de tarefa alheia")
        void deveLancarExcecaoParaTarefaAlheia() {
            UUID id = UUID.randomUUID();
            when(tarefaRepository.findByIdAndUsuarioId(id, "ana-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tarefaService.listarHistorico(id))
                    .isInstanceOf(TarefaNaoEncontradaException.class);
            verify(historicoRepository, never()).findByTarefaIdOrderByDataHoraDesc(any());
        }
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        @Test
        @DisplayName("Deve criar tarefa com dono, data de abertura, situação aberta e última posição")
        void deveCriarTarefaComPadroes() {
            when(tarefaRepository.countByUsuarioId("ana-id")).thenReturn(2);
            when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(chamada -> chamada.getArgument(0));
            TarefaRequisicaoDTO requisicao = new TarefaRequisicaoDTO(
                    "Pagar conta de luz", Prioridade.ALTA, LocalDate.of(2026, 9, 30), new BigDecimal("250"));

            TarefaRespostaDTO resposta = tarefaService.criar(requisicao);

            ArgumentCaptor<Tarefa> captor = ArgumentCaptor.forClass(Tarefa.class);
            verify(tarefaRepository).save(captor.capture());
            Tarefa salva = captor.getValue();
            assertThat(salva.getId()).isNotNull();
            assertThat(salva.getUsuarioId()).isEqualTo("ana-id");
            assertThat(salva.getSituacao()).isEqualTo(Situacao.ABERTA);
            assertThat(salva.getDataAbertura()).isEqualTo(AGORA);
            assertThat(salva.getPosicao()).isEqualTo(3);
            assertThat(salva.getValor()).isEqualTo(new BigDecimal("250.00"));
            verify(historicoService).registrarCriacao(salva, ANA);
            assertThat(resposta.id()).isEqualTo(salva.getId());
            assertThat(resposta.posicao()).isEqualTo(3);
            assertThat(resposta.atrasada()).isFalse();
        }

        @Test
        @DisplayName("Deve criar tarefa com valor zero quando não informado")
        void deveCriarComValorZeroQuandoNaoInformado() {
            when(tarefaRepository.countByUsuarioId("ana-id")).thenReturn(0);
            when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(chamada -> chamada.getArgument(0));

            TarefaRespostaDTO resposta = tarefaService.criar(
                    new TarefaRequisicaoDTO("Ler livro", Prioridade.BAIXA, null, null));

            assertThat(resposta.valor()).isEqualTo(new BigDecimal("0.00"));
            assertThat(resposta.posicao()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("listar e obter")
    class ListarEObter {

        @Test
        @DisplayName("Deve listar todas as tarefas do usuário em ordem de posição")
        void deveListarTodasAsTarefasDoUsuario() {
            Tarefa primeira = tarefaBase().posicao(1).build();
            Tarefa segunda = tarefaBase().posicao(2).dataPrevistaConclusao(HOJE.minusDays(1)).build();
            when(tarefaRepository.findByUsuarioIdOrderByPosicaoAsc("ana-id")).thenReturn(List.of(primeira, segunda));

            List<TarefaRespostaDTO> tarefas = tarefaService.listar(FiltroSituacao.TODAS, null);

            assertThat(tarefas).extracting(TarefaRespostaDTO::posicao).containsExactly(1, 2);
            assertThat(tarefas.get(1).atrasada()).isTrue();
        }

        @Test
        @DisplayName("Deve listar apenas abertas quando o filtro for ABERTAS sem busca")
        void deveListarApenasAbertasSemBusca() {
            Tarefa aberta = tarefaBase().build();
            when(tarefaRepository.findByUsuarioIdAndSituacaoOrderByPosicaoAsc("ana-id", Situacao.ABERTA))
                    .thenReturn(List.of(aberta));

            List<TarefaRespostaDTO> tarefas = tarefaService.listar(FiltroSituacao.ABERTAS, "   ");

            assertThat(tarefas).hasSize(1);
            verify(tarefaRepository, never()).buscar(any(), any());
        }

        @Test
        @DisplayName("Deve normalizar o termo e buscar em todas as tarefas quando o filtro for TODAS")
        void deveNormalizarTermoEBuscarEmTodas() {
            when(tarefaRepository.buscar("ana-id", "lampada")).thenReturn(List.of(tarefaBase().build()));

            List<TarefaRespostaDTO> tarefas = tarefaService.listar(FiltroSituacao.TODAS, " Lâmpada ");

            assertThat(tarefas).hasSize(1);
            verify(tarefaRepository, never()).findByUsuarioIdOrderByPosicaoAsc(any());
        }

        @Test
        @DisplayName("Deve combinar filtro de situação com termo de busca")
        void deveCombinarFiltroComBusca() {
            when(tarefaRepository.buscarPorSituacao("ana-id", Situacao.CONCLUIDA, "x")).thenReturn(List.of());

            List<TarefaRespostaDTO> tarefas = tarefaService.listar(FiltroSituacao.CONCLUIDAS, "x");

            assertThat(tarefas).isEmpty();
            verify(tarefaRepository).buscarPorSituacao("ana-id", Situacao.CONCLUIDA, "x");
        }

        @Test
        @DisplayName("Deve obter tarefa do usuário pelo identificador")
        void deveObterTarefaDoUsuario() {
            Tarefa tarefa = tarefaBase().build();
            when(tarefaRepository.findByIdAndUsuarioId(tarefa.getId(), "ana-id")).thenReturn(Optional.of(tarefa));

            TarefaRespostaDTO resposta = tarefaService.obter(tarefa.getId());

            assertThat(resposta.id()).isEqualTo(tarefa.getId());
            assertThat(resposta.descricao()).isEqualTo("Pagar conta de luz");
        }

        @Test
        @DisplayName("Deve lançar exceção ao obter tarefa inexistente ou de outro usuário")
        void deveLancarExcecaoAoObterTarefaInexistente() {
            UUID id = UUID.randomUUID();
            when(tarefaRepository.findByIdAndUsuarioId(id, "ana-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tarefaService.obter(id)).isInstanceOf(TarefaNaoEncontradaException.class);
            verify(historicoService, never()).registrarCriacao(any(), eq(ANA));
        }
    }

    @Nested
    @DisplayName("editar")
    class Editar {

        @Test
        @DisplayName("Deve aplicar os campos editáveis, preservar os demais e registrar alterações")
        void deveAplicarCamposEditaveisERegistrarAlteracoes() {
            Tarefa existente = tarefaBase().posicao(4).build();
            LocalDateTime aberturaOriginal = existente.getDataAbertura();
            when(tarefaRepository.findByIdAndUsuarioId(existente.getId(), "ana-id")).thenReturn(Optional.of(existente));
            when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(chamada -> chamada.getArgument(0));
            TarefaRequisicaoDTO requisicao = new TarefaRequisicaoDTO(
                    "Pagar conta de energia", Prioridade.BAIXA, LocalDate.of(2026, 10, 5), new BigDecimal("70"));

            TarefaRespostaDTO resposta = tarefaService.editar(existente.getId(), requisicao);

            ArgumentCaptor<Tarefa> anteriorCaptor = ArgumentCaptor.forClass(Tarefa.class);
            verify(historicoService).registrarAlteracoes(anteriorCaptor.capture(), eq(existente), eq(ANA));
            Tarefa anterior = anteriorCaptor.getValue();
            assertThat(anterior.getDescricao()).isEqualTo("Pagar conta de luz");
            assertThat(anterior.getValor()).isEqualByComparingTo("50.00");
            assertThat(resposta.descricao()).isEqualTo("Pagar conta de energia");
            assertThat(resposta.prioridade()).isEqualTo(Prioridade.BAIXA);
            assertThat(resposta.dataPrevistaConclusao()).isEqualTo(LocalDate.of(2026, 10, 5));
            assertThat(resposta.valor()).isEqualTo(new BigDecimal("70.00"));
            assertThat(resposta.posicao()).isEqualTo(4);
            assertThat(resposta.dataAbertura()).isEqualTo(aberturaOriginal);
            assertThat(resposta.situacao()).isEqualTo(Situacao.ABERTA);
        }

        @Test
        @DisplayName("Deve permitir editar tarefa concluída")
        void devePermitirEditarTarefaConcluida() {
            Tarefa concluida = tarefaBase().situacao(Situacao.CONCLUIDA).dataConclusaoReal(AGORA).build();
            when(tarefaRepository.findByIdAndUsuarioId(concluida.getId(), "ana-id")).thenReturn(Optional.of(concluida));
            when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(chamada -> chamada.getArgument(0));

            TarefaRespostaDTO resposta = tarefaService.editar(concluida.getId(),
                    new TarefaRequisicaoDTO("Nova", Prioridade.ALTA, null, null));

            assertThat(resposta.descricao()).isEqualTo("Nova");
            assertThat(resposta.situacao()).isEqualTo(Situacao.CONCLUIDA);
            assertThat(resposta.dataConclusaoReal()).isEqualTo(AGORA);
            assertThat(resposta.valor()).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        @DisplayName("Deve lançar exceção ao editar tarefa de outro usuário")
        void deveLancarExcecaoAoEditarTarefaAlheia() {
            UUID id = UUID.randomUUID();
            when(tarefaRepository.findByIdAndUsuarioId(id, "ana-id")).thenReturn(Optional.empty());
            TarefaRequisicaoDTO requisicao = new TarefaRequisicaoDTO("X", Prioridade.ALTA, null, null);

            assertThatThrownBy(() -> tarefaService.editar(id, requisicao))
                    .isInstanceOf(TarefaNaoEncontradaException.class);
            verify(tarefaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("concluir e reabrir")
    class ConcluirEReabrir {

        @Test
        @DisplayName("Deve concluir tarefa aberta registrando data real, histórico e movendo-a para o final")
        void deveConcluirTarefaAberta() {
            Tarefa aberta = tarefaBase().posicao(2).build();
            when(tarefaRepository.findByIdAndUsuarioId(aberta.getId(), "ana-id")).thenReturn(Optional.of(aberta));
            when(tarefaRepository.countByUsuarioId("ana-id")).thenReturn(5);
            when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(chamada -> chamada.getArgument(0));

            TarefaRespostaDTO resposta = tarefaService.concluir(aberta.getId());

            assertThat(resposta.situacao()).isEqualTo(Situacao.CONCLUIDA);
            assertThat(resposta.dataConclusaoReal()).isEqualTo(AGORA);
            assertThat(resposta.atrasada()).isFalse();
            assertThat(resposta.posicao()).isEqualTo(5);
            verify(tarefaRepository).decrementarPosicoes("ana-id", 3, 5);
            verify(historicoService).registrarMudancaSituacao(aberta, Situacao.ABERTA, Situacao.CONCLUIDA, ANA);
        }

        @Test
        @DisplayName("Concluir tarefa que já está no final não deve deslocar as demais")
        void concluirTarefaNoFinalNaoDeveDeslocar() {
            Tarefa ultima = tarefaBase().posicao(3).build();
            when(tarefaRepository.findByIdAndUsuarioId(ultima.getId(), "ana-id")).thenReturn(Optional.of(ultima));
            when(tarefaRepository.countByUsuarioId("ana-id")).thenReturn(3);
            when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(chamada -> chamada.getArgument(0));

            TarefaRespostaDTO resposta = tarefaService.concluir(ultima.getId());

            assertThat(resposta.posicao()).isEqualTo(3);
            verify(tarefaRepository, never()).decrementarPosicoes(any(), anyInt(), anyInt());
            verify(tarefaRepository, never()).incrementarPosicoes(any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("Concluir tarefa já concluída deve ser sem efeito")
        void concluirTarefaJaConcluidaDeveSerSemEfeito() {
            LocalDateTime conclusaoOriginal = AGORA.minusDays(2);
            Tarefa concluida = tarefaBase().situacao(Situacao.CONCLUIDA).dataConclusaoReal(conclusaoOriginal).build();
            when(tarefaRepository.findByIdAndUsuarioId(concluida.getId(), "ana-id")).thenReturn(Optional.of(concluida));

            TarefaRespostaDTO resposta = tarefaService.concluir(concluida.getId());

            assertThat(resposta.dataConclusaoReal()).isEqualTo(conclusaoOriginal);
            verify(tarefaRepository, never()).save(any());
            verify(historicoService, never()).registrarMudancaSituacao(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Deve reabrir tarefa concluída apagando a data real, registrando histórico e mantendo a posição")
        void deveReabrirTarefaConcluida() {
            Tarefa concluida = tarefaBase().situacao(Situacao.CONCLUIDA).dataConclusaoReal(AGORA).posicao(4).build();
            when(tarefaRepository.findByIdAndUsuarioId(concluida.getId(), "ana-id")).thenReturn(Optional.of(concluida));
            when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(chamada -> chamada.getArgument(0));

            TarefaRespostaDTO resposta = tarefaService.reabrir(concluida.getId());

            assertThat(resposta.situacao()).isEqualTo(Situacao.ABERTA);
            assertThat(resposta.dataConclusaoReal()).isNull();
            assertThat(resposta.posicao()).isEqualTo(4);
            verify(tarefaRepository, never()).decrementarPosicoes(any(), anyInt(), anyInt());
            verify(historicoService).registrarMudancaSituacao(concluida, Situacao.CONCLUIDA, Situacao.ABERTA, ANA);
        }

        @Test
        @DisplayName("Reabrir tarefa já aberta deve ser sem efeito")
        void reabrirTarefaJaAbertaDeveSerSemEfeito() {
            Tarefa aberta = tarefaBase().build();
            when(tarefaRepository.findByIdAndUsuarioId(aberta.getId(), "ana-id")).thenReturn(Optional.of(aberta));

            TarefaRespostaDTO resposta = tarefaService.reabrir(aberta.getId());

            assertThat(resposta.situacao()).isEqualTo(Situacao.ABERTA);
            verify(tarefaRepository, never()).save(any());
            verify(historicoService, never()).registrarMudancaSituacao(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Concluir e reabrir tarefa alheia devem lançar exceção")
        void concluirEReabrirTarefaAlheiaDevemLancarExcecao() {
            UUID id = UUID.randomUUID();
            when(tarefaRepository.findByIdAndUsuarioId(id, "ana-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tarefaService.concluir(id)).isInstanceOf(TarefaNaoEncontradaException.class);
            assertThatThrownBy(() -> tarefaService.reabrir(id)).isInstanceOf(TarefaNaoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("mover")
    class Mover {

        @Test
        @DisplayName("Mover para o topo deve deslocar as anteriores para baixo")
        void moverParaTopoDeveDeslocarAnteriores() {
            Tarefa tarefa = prepararMover(5, 5);

            TarefaRespostaDTO resposta = tarefaService.mover(tarefa.getId(),
                    new MoverTarefaRequisicaoDTO(DestinoMovimento.TOPO, null));

            verify(tarefaRepository).incrementarPosicoes("ana-id", 1, 4);
            verify(tarefaRepository, never()).decrementarPosicoes(any(), anyInt(), anyInt());
            assertThat(resposta.posicao()).isEqualTo(1);
            verify(historicoService, never()).registrarAlteracoes(any(), any(), any());
        }

        @Test
        @DisplayName("Mover para o final deve deslocar as seguintes para cima")
        void moverParaFinalDeveDeslocarSeguintes() {
            Tarefa tarefa = prepararMover(1, 5);

            TarefaRespostaDTO resposta = tarefaService.mover(tarefa.getId(),
                    new MoverTarefaRequisicaoDTO(DestinoMovimento.FINAL, 99));

            verify(tarefaRepository).decrementarPosicoes("ana-id", 2, 5);
            verify(tarefaRepository, never()).incrementarPosicoes(any(), anyInt(), anyInt());
            assertThat(resposta.posicao()).isEqualTo(5);
        }

        @Test
        @DisplayName("Mover para posição específica acima deve deslocar o intervalo")
        void moverParaPosicaoEspecifica() {
            Tarefa tarefa = prepararMover(4, 5);

            TarefaRespostaDTO resposta = tarefaService.mover(tarefa.getId(),
                    new MoverTarefaRequisicaoDTO(DestinoMovimento.POSICAO, 2));

            verify(tarefaRepository).incrementarPosicoes("ana-id", 2, 3);
            assertThat(resposta.posicao()).isEqualTo(2);
        }

        @Test
        @DisplayName("Mover para a própria posição deve ser sem efeito")
        void moverParaPropriaPosicaoDeveSerSemEfeito() {
            Tarefa tarefa = prepararMover(3, 5);

            TarefaRespostaDTO resposta = tarefaService.mover(tarefa.getId(),
                    new MoverTarefaRequisicaoDTO(DestinoMovimento.POSICAO, 3));

            verify(tarefaRepository, never()).incrementarPosicoes(any(), anyInt(), anyInt());
            verify(tarefaRepository, never()).decrementarPosicoes(any(), anyInt(), anyInt());
            verify(tarefaRepository, never()).save(any());
            assertThat(resposta.posicao()).isEqualTo(3);
        }

        @Test
        @DisplayName("Posição nula, zero ou acima do total deve lançar PosicaoInvalidaException")
        void posicaoForaDoIntervaloDeveLancarExcecao() {
            Tarefa tarefa = prepararMover(2, 5);
            UUID id = tarefa.getId();

            for (Integer posicao : new Integer[] {null, 0, 6}) {
                MoverTarefaRequisicaoDTO requisicao = new MoverTarefaRequisicaoDTO(DestinoMovimento.POSICAO, posicao);
                assertThatThrownBy(() -> tarefaService.mover(id, requisicao))
                        .isInstanceOf(PosicaoInvalidaException.class)
                        .hasMessage("A posição deve estar entre 1 e 5.");
            }
            verify(tarefaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Mover tarefa alheia deve lançar TarefaNaoEncontradaException")
        void moverTarefaAlheiaDeveLancarExcecao() {
            UUID id = UUID.randomUUID();
            when(tarefaRepository.buscarParaMover(id, "ana-id")).thenReturn(Optional.empty());
            MoverTarefaRequisicaoDTO requisicao = new MoverTarefaRequisicaoDTO(DestinoMovimento.TOPO, null);

            assertThatThrownBy(() -> tarefaService.mover(id, requisicao))
                    .isInstanceOf(TarefaNaoEncontradaException.class);
        }

        private Tarefa prepararMover(int posicaoAtual, int total) {
            Tarefa tarefa = tarefaBase().posicao(posicaoAtual).build();
            when(tarefaRepository.buscarParaMover(tarefa.getId(), "ana-id")).thenReturn(Optional.of(tarefa));
            when(tarefaRepository.countByUsuarioId("ana-id")).thenReturn(total);
            lenient().when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(chamada -> chamada.getArgument(0));
            return tarefa;
        }
    }

    @Nested
    @DisplayName("excluir")
    class Excluir {

        @Test
        @DisplayName("Deve excluir a tarefa e fechar a lacuna de posições")
        void deveExcluirEFecharLacuna() {
            Tarefa tarefa = tarefaBase().posicao(2).build();
            when(tarefaRepository.findByIdAndUsuarioId(tarefa.getId(), "ana-id")).thenReturn(Optional.of(tarefa));

            tarefaService.excluir(tarefa.getId());

            InOrder ordem = inOrder(tarefaRepository);
            ordem.verify(tarefaRepository).delete(tarefa);
            ordem.verify(tarefaRepository).decrementarPosicoesApos("ana-id", 2);
            verify(historicoService, never()).registrarAlteracoes(any(), any(), any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao excluir tarefa alheia")
        void deveLancarExcecaoAoExcluirTarefaAlheia() {
            UUID id = UUID.randomUUID();
            when(tarefaRepository.findByIdAndUsuarioId(id, "ana-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tarefaService.excluir(id)).isInstanceOf(TarefaNaoEncontradaException.class);
            verify(tarefaRepository, never()).delete(any(Tarefa.class));
            verify(tarefaRepository, never()).decrementarPosicoesApos(any(), anyInt());
        }
    }

    private static Tarefa.TarefaBuilder tarefaBase() {
        return Tarefa.builder()
                .id(UUID.randomUUID())
                .usuarioId("ana-id")
                .descricao("Pagar conta de luz")
                .prioridade(Prioridade.ALTA)
                .situacao(Situacao.ABERTA)
                .dataAbertura(AGORA.minusDays(1))
                .dataPrevistaConclusao(LocalDate.of(2026, 9, 30))
                .valor(new BigDecimal("50.00"))
                .posicao(1);
    }
}
