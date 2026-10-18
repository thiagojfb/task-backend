package com.task.task_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.task.task_backend.entity.Prioridade;
import com.task.task_backend.entity.RegistroHistorico;
import com.task.task_backend.entity.Situacao;
import com.task.task_backend.entity.Tarefa;
import com.task.task_backend.entity.TipoRegistro;
import com.task.task_backend.repository.RegistroHistoricoRepository;
import com.task.task_backend.repository.TarefaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Repositórios (integração com H2 + Flyway)")
class TarefaRepositoryIT {

    private static final String ANA = "ana-id";
    private static final String BRUNO = "bruno-id";

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private RegistroHistoricoRepository historicoRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void limpar() {
        historicoRepository.deleteAll();
        tarefaRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve listar tarefas do usuário ordenadas por posição")
    void deveListarTarefasDoUsuarioOrdenadasPorPosicao() {
        salvar(ANA, "Terceira", 3);
        salvar(ANA, "Primeira", 1);
        salvar(ANA, "Segunda", 2);
        salvar(BRUNO, "De Bruno", 1);

        List<Tarefa> tarefas = tarefaRepository.findByUsuarioIdOrderByPosicaoAsc(ANA);

        assertThat(tarefas).extracting(Tarefa::getDescricao).containsExactly("Primeira", "Segunda", "Terceira");
    }

    @Test
    @DisplayName("Deve encontrar tarefa somente pelo dono")
    void deveEncontrarTarefaSomentePeloDono() {
        Tarefa tarefa = salvar(ANA, "Minha", 1);

        assertThat(tarefaRepository.findByIdAndUsuarioId(tarefa.getId(), ANA)).isPresent();
        assertThat(tarefaRepository.findByIdAndUsuarioId(tarefa.getId(), BRUNO)).isEmpty();
    }

    @Test
    @DisplayName("Deve contar tarefas por usuário")
    void deveContarTarefasPorUsuario() {
        salvar(ANA, "A", 1);
        salvar(ANA, "B", 2);
        salvar(BRUNO, "C", 1);

        assertThat(tarefaRepository.countByUsuarioId(ANA)).isEqualTo(2);
        assertThat(tarefaRepository.countByUsuarioId("ninguem")).isZero();
    }

    @Test
    @DisplayName("Deve listar histórico do mais recente ao mais antigo")
    void deveListarHistoricoDoMaisRecenteAoMaisAntigo() {
        Tarefa tarefa = salvar(ANA, "Com histórico", 1);
        LocalDateTime base = LocalDateTime.of(2026, 9, 15, 10, 0);
        historicoRepository.save(registro(tarefa, base, TipoRegistro.CRIACAO));
        historicoRepository.save(registro(tarefa, base.plusHours(2), TipoRegistro.CONCLUSAO));
        historicoRepository.save(registro(tarefa, base.plusHours(1), TipoRegistro.ALTERACAO));

        List<RegistroHistorico> historico = historicoRepository.findByTarefaIdOrderByDataHoraDesc(tarefa.getId());

        assertThat(historico).extracting(RegistroHistorico::getTipo)
                .containsExactly(TipoRegistro.CONCLUSAO, TipoRegistro.ALTERACAO, TipoRegistro.CRIACAO);
    }

    @Test
    @DisplayName("Deve incrementar posições apenas no intervalo e apenas do usuário")
    void deveIncrementarPosicoesNoIntervalo() {
        for (int posicao = 1; posicao <= 5; posicao++) {
            salvar(ANA, "A" + posicao, posicao);
        }
        salvar(BRUNO, "B1", 3);

        int afetadas = tarefaRepository.incrementarPosicoes(ANA, 2, 4);

        assertThat(afetadas).isEqualTo(3);
        assertThat(tarefaRepository.findByUsuarioIdOrderByPosicaoAsc(ANA))
                .extracting(Tarefa::getDescricao, Tarefa::getPosicao)
                .containsExactly(tuple("A1", 1), tuple("A2", 3), tuple("A3", 4), tuple("A4", 5), tuple("A5", 5));
        assertThat(tarefaRepository.findByUsuarioIdOrderByPosicaoAsc(BRUNO).get(0).getPosicao()).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve decrementar posições apenas no intervalo")
    void deveDecrementarPosicoesNoIntervalo() {
        for (int posicao = 1; posicao <= 5; posicao++) {
            salvar(ANA, "A" + posicao, posicao);
        }

        int afetadas = tarefaRepository.decrementarPosicoes(ANA, 2, 4);

        assertThat(afetadas).isEqualTo(3);
        assertThat(tarefaRepository.findByUsuarioIdOrderByPosicaoAsc(ANA))
                .extracting(Tarefa::getDescricao, Tarefa::getPosicao)
                .containsExactly(tuple("A1", 1), tuple("A2", 1), tuple("A3", 2), tuple("A4", 3), tuple("A5", 5));
    }

    @Test
    @DisplayName("Deve buscar tarefa para mover somente do dono")
    void deveBuscarTarefaParaMoverSomenteDoDono() {
        Tarefa tarefa = salvar(ANA, "Mover", 1);

        assertThat(tarefaRepository.buscarParaMover(tarefa.getId(), ANA)).contains(tarefa);
        assertThat(tarefaRepository.buscarParaMover(tarefa.getId(), BRUNO)).isEmpty();
    }

    @Test
    @DisplayName("Deve decrementar apenas as posições posteriores à excluída")
    void deveDecrementarPosicoesAposExcluida() {
        for (int posicao = 1; posicao <= 4; posicao++) {
            salvar(ANA, "A" + posicao, posicao);
        }
        salvar(BRUNO, "B1", 4);

        int afetadas = tarefaRepository.decrementarPosicoesApos(ANA, 2);

        assertThat(afetadas).isEqualTo(2);
        assertThat(tarefaRepository.findByUsuarioIdOrderByPosicaoAsc(ANA))
                .extracting(Tarefa::getDescricao, Tarefa::getPosicao)
                .containsExactly(tuple("A1", 1), tuple("A2", 2), tuple("A3", 2), tuple("A4", 3));
        assertThat(tarefaRepository.findByUsuarioIdOrderByPosicaoAsc(BRUNO).get(0).getPosicao()).isEqualTo(4);
    }

    @Test
    @DisplayName("Excluir tarefa deve remover o histórico em cascata")
    void excluirTarefaDeveRemoverHistoricoEmCascata() {
        Tarefa tarefa = salvar(ANA, "Com histórico", 1);
        historicoRepository.save(registro(tarefa, LocalDateTime.of(2026, 9, 15, 10, 0), TipoRegistro.CRIACAO));
        historicoRepository.flush();
        entityManager.clear();

        tarefaRepository.deleteById(tarefa.getId());
        tarefaRepository.flush();

        assertThat(historicoRepository.findByTarefaIdOrderByDataHoraDesc(tarefa.getId())).isEmpty();
        assertThat(tarefaRepository.findById(tarefa.getId())).isEmpty();
    }

    @Test
    @DisplayName("Busca normalizada deve ignorar acentos e maiúsculas e respeitar a ordem de posição")
    void buscaNormalizadaDeveIgnorarAcentosEMaiusculas() {
        salvar(ANA, "Comprar lâmpada", 2);
        salvar(ANA, "Pagar luz", 1);
        salvar(ANA, "Ler livro", 3);
        salvar(BRUNO, "Pagar luz do Bruno", 1);

        assertThat(tarefaRepository.buscar(ANA, "lu")).extracting(Tarefa::getDescricao).containsExactly("Pagar luz");
        assertThat(tarefaRepository.buscar(ANA, "lampada")).extracting(Tarefa::getDescricao)
                .containsExactly("Comprar lâmpada");
        assertThat(tarefaRepository.buscar(ANA, "l")).extracting(Tarefa::getDescricao)
                .containsExactly("Pagar luz", "Comprar lâmpada", "Ler livro");
        assertThat(tarefaRepository.buscar(ANA, "inexistente")).isEmpty();
    }

    @Test
    @DisplayName("Busca por situação deve combinar filtro e termo")
    void buscaPorSituacaoDeveCombinarFiltroETermo() {
        salvar(ANA, "Pagar luz", 1);
        Tarefa concluida = salvar(ANA, "Pagar água", 2);
        concluida.setSituacao(Situacao.CONCLUIDA);
        tarefaRepository.saveAndFlush(concluida);

        assertThat(tarefaRepository.buscarPorSituacao(ANA, Situacao.ABERTA, "pagar"))
                .extracting(Tarefa::getDescricao).containsExactly("Pagar luz");
        assertThat(tarefaRepository.buscarPorSituacao(ANA, Situacao.CONCLUIDA, "pagar"))
                .extracting(Tarefa::getDescricao).containsExactly("Pagar água");
        assertThat(tarefaRepository.findByUsuarioIdAndSituacaoOrderByPosicaoAsc(ANA, Situacao.CONCLUIDA))
                .extracting(Tarefa::getDescricao).containsExactly("Pagar água");
    }

    @Test
    @DisplayName("Soma do período deve considerar apenas tarefas abertas com prazo no intervalo")
    void somaDoPeriodoDeveConsiderarApenasAbertasNoIntervalo() {
        LocalDate inicio = LocalDate.of(2026, 9, 1);
        LocalDate fim = LocalDate.of(2026, 9, 30);
        salvarComValor(ANA, "Aberta no mês", 1, Situacao.ABERTA, LocalDate.of(2026, 9, 10), "1000.00");
        salvarComValor(ANA, "Aberta no último dia", 2, Situacao.ABERTA, fim, "250.00");
        salvarComValor(ANA, "Concluída no mês", 3, Situacao.CONCLUIDA, LocalDate.of(2026, 9, 15), "500.00");
        salvarComValor(ANA, "Aberta mês seguinte", 4, Situacao.ABERTA, LocalDate.of(2026, 10, 1), "300.00");
        salvarComValor(ANA, "Aberta sem prazo", 5, Situacao.ABERTA, null, "99.00");
        salvarComValor(BRUNO, "De Bruno no mês", 1, Situacao.ABERTA, LocalDate.of(2026, 9, 5), "77.00");

        assertThat(tarefaRepository.somarValorAbertasNoPeriodo(ANA, inicio, fim)).isEqualByComparingTo("1250.00");
        assertThat(tarefaRepository.somarValorAbertasNoPeriodo("ninguem", inicio, fim)).isEqualByComparingTo("0");
    }

    private void salvarComValor(String usuarioId, String descricao, int posicao, Situacao situacao,
            LocalDate prazo, String valor) {
        tarefaRepository.save(Tarefa.builder()
                .id(UUID.randomUUID())
                .usuarioId(usuarioId)
                .descricao(descricao)
                .prioridade(Prioridade.MEDIA)
                .situacao(situacao)
                .dataAbertura(LocalDateTime.of(2026, 9, 1, 8, 0))
                .dataPrevistaConclusao(prazo)
                .valor(new BigDecimal(valor))
                .posicao(posicao)
                .build());
    }

    private Tarefa salvar(String usuarioId, String descricao, int posicao) {
        return tarefaRepository.save(Tarefa.builder()
                .id(UUID.randomUUID())
                .usuarioId(usuarioId)
                .descricao(descricao)
                .prioridade(Prioridade.MEDIA)
                .situacao(Situacao.ABERTA)
                .dataAbertura(LocalDateTime.of(2026, 9, 1, 8, 0))
                .dataPrevistaConclusao(LocalDate.of(2026, 9, 30))
                .valor(BigDecimal.TEN)
                .posicao(posicao)
                .build());
    }

    private static RegistroHistorico registro(Tarefa tarefa, LocalDateTime dataHora, TipoRegistro tipo) {
        return RegistroHistorico.builder()
                .id(UUID.randomUUID())
                .tarefa(tarefa)
                .usuarioId(ANA)
                .usuarioNome("Ana Silva")
                .dataHora(dataHora)
                .tipo(tipo)
                .build();
    }
}
