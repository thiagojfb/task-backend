package com.task.task_backend.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.task.task_backend.entity.Prioridade;
import com.task.task_backend.entity.RegistroHistorico;
import com.task.task_backend.entity.Situacao;
import com.task.task_backend.entity.Tarefa;
import com.task.task_backend.entity.TipoRegistro;
import com.task.task_backend.repository.RegistroHistoricoRepository;
import com.task.task_backend.seguranca.UsuarioLogado;
import com.task.task_backend.service.HistoricoService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoricoService")
class HistoricoServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 19, 14, 30, 0);
    private static final UsuarioLogado ANA = new UsuarioLogado("ana-id", "Ana Silva");

    @Mock
    private RegistroHistoricoRepository historicoRepository;

    private HistoricoService historicoService;

    @BeforeEach
    void configurar() {
        Clock relogio = Clock.fixed(AGORA.atZone(FUSO).toInstant(), FUSO);
        historicoService = new HistoricoService(historicoRepository, relogio);
    }

    @Test
    @DisplayName("Deve registrar criação com tipo CRIACAO e sem campo/valores")
    void deveRegistrarCriacao() {
        Tarefa tarefa = tarefaBase().build();

        historicoService.registrarCriacao(tarefa, ANA);

        RegistroHistorico registro = capturarUnico();
        assertThat(registro.getId()).isNotNull();
        assertThat(registro.getTarefa()).isSameAs(tarefa);
        assertThat(registro.getTipo()).isEqualTo(TipoRegistro.CRIACAO);
        assertThat(registro.getUsuarioId()).isEqualTo("ana-id");
        assertThat(registro.getUsuarioNome()).isEqualTo("Ana Silva");
        assertThat(registro.getDataHora()).isEqualTo(AGORA);
        assertThat(registro.getCampo()).isNull();
        assertThat(registro.getValorAnterior()).isNull();
        assertThat(registro.getValorNovo()).isNull();
    }

    @Test
    @DisplayName("Deve registrar uma alteração por campo modificado com valores formatados")
    void deveRegistrarUmaAlteracaoPorCampoModificado() {
        Tarefa anterior = tarefaBase().build();
        Tarefa atual = tarefaBase()
                .id(anterior.getId())
                .descricao("Pagar conta de energia")
                .prioridade(Prioridade.BAIXA)
                .dataPrevistaConclusao(null)
                .valor(new BigDecimal("70.00"))
                .build();

        historicoService.registrarAlteracoes(anterior, atual, ANA);

        List<RegistroHistorico> registros = capturarTodos();
        assertThat(registros).hasSize(4).allSatisfy(registro -> {
            assertThat(registro.getTipo()).isEqualTo(TipoRegistro.ALTERACAO);
            assertThat(registro.getTarefa()).isSameAs(atual);
            assertThat(registro.getUsuarioNome()).isEqualTo("Ana Silva");
            assertThat(registro.getDataHora()).isEqualTo(AGORA);
        });
        assertThat(registros).extracting(RegistroHistorico::getCampo, RegistroHistorico::getValorAnterior,
                        RegistroHistorico::getValorNovo)
                .containsExactly(
                        tuple("descricao", "Pagar conta de luz", "Pagar conta de energia"),
                        tuple("prioridade", "Alta", "Baixa"),
                        tuple("dataPrevistaConclusao", "30/09/2026", "—"),
                        tuple("valor", "R$ 50,00", "R$ 70,00"));
    }

    @Test
    @DisplayName("Não deve registrar nada quando nenhum campo mudou, mesmo com escala diferente no valor")
    void naoDeveRegistrarQuandoNadaMudou() {
        Tarefa anterior = tarefaBase().valor(new BigDecimal("50.0")).build();
        Tarefa atual = tarefaBase().id(anterior.getId()).valor(new BigDecimal("50.00")).build();

        historicoService.registrarAlteracoes(anterior, atual, ANA);

        verify(historicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve registrar somente o campo alterado quando um único campo muda")
    void deveRegistrarSomenteOCampoAlterado() {
        Tarefa anterior = tarefaBase().build();
        Tarefa atual = tarefaBase().id(anterior.getId()).valor(new BigDecimal("70")).build();

        historicoService.registrarAlteracoes(anterior, atual, ANA);

        RegistroHistorico registro = capturarUnico();
        assertThat(registro.getCampo()).isEqualTo("valor");
        assertThat(registro.getValorAnterior()).isEqualTo("R$ 50,00");
        assertThat(registro.getValorNovo()).isEqualTo("R$ 70,00");
    }

    @Test
    @DisplayName("Deve registrar conclusão como mudança de situação Aberta → Concluída")
    void deveRegistrarConclusao() {
        Tarefa tarefa = tarefaBase().situacao(Situacao.CONCLUIDA).build();

        historicoService.registrarMudancaSituacao(tarefa, Situacao.ABERTA, Situacao.CONCLUIDA, ANA);

        RegistroHistorico registro = capturarUnico();
        assertThat(registro.getTipo()).isEqualTo(TipoRegistro.CONCLUSAO);
        assertThat(registro.getCampo()).isEqualTo("situacao");
        assertThat(registro.getValorAnterior()).isEqualTo("Aberta");
        assertThat(registro.getValorNovo()).isEqualTo("Concluída");
    }

    @Test
    @DisplayName("Deve registrar reabertura como mudança de situação Concluída → Aberta")
    void deveRegistrarReabertura() {
        Tarefa tarefa = tarefaBase().build();

        historicoService.registrarMudancaSituacao(tarefa, Situacao.CONCLUIDA, Situacao.ABERTA, ANA);

        RegistroHistorico registro = capturarUnico();
        assertThat(registro.getTipo()).isEqualTo(TipoRegistro.REABERTURA);
        assertThat(registro.getValorAnterior()).isEqualTo("Concluída");
        assertThat(registro.getValorNovo()).isEqualTo("Aberta");
    }

    private List<RegistroHistorico> capturarTodos() {
        ArgumentCaptor<RegistroHistorico> captor = ArgumentCaptor.forClass(RegistroHistorico.class);
        verify(historicoRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private RegistroHistorico capturarUnico() {
        ArgumentCaptor<RegistroHistorico> captor = ArgumentCaptor.forClass(RegistroHistorico.class);
        verify(historicoRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Tarefa.TarefaBuilder tarefaBase() {
        return Tarefa.builder()
                .id(UUID.randomUUID())
                .usuarioId("ana-id")
                .descricao("Pagar conta de luz")
                .prioridade(Prioridade.ALTA)
                .situacao(Situacao.ABERTA)
                .dataAbertura(AGORA)
                .dataPrevistaConclusao(LocalDate.of(2026, 9, 30))
                .valor(new BigDecimal("50.00"))
                .posicao(1);
    }
}
