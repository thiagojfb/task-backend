package com.task.task_backend.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.task.task_backend.dto.ResumoFinanceiroRespostaDTO;
import com.task.task_backend.repository.TarefaRepository;
import com.task.task_backend.seguranca.UsuarioAutenticado;
import com.task.task_backend.seguranca.UsuarioLogado;
import com.task.task_backend.service.ResumoFinanceiroService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumoFinanceiroService")
class ResumoFinanceiroServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final UsuarioLogado ANA = new UsuarioLogado("ana-id", "Ana Silva");

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private UsuarioAutenticado usuarioAutenticado;

    @Test
    @DisplayName("Deve somar as tarefas abertas do mês corrente e formatar o total")
    void deveSomarTarefasAbertasDoMesCorrente() {
        ResumoFinanceiroService servico = servicoEm(LocalDateTime.of(2026, 9, 19, 10, 0));
        when(usuarioAutenticado.obter()).thenReturn(ANA);
        when(tarefaRepository.somarValorAbertasNoPeriodo("ana-id", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(new BigDecimal("1250"));

        ResumoFinanceiroRespostaDTO resumo = servico.obter();

        assertThat(resumo.mes()).isEqualTo(9);
        assertThat(resumo.ano()).isEqualTo(2026);
        assertThat(resumo.totalAGastar()).isEqualTo(new BigDecimal("1250.00"));
        assertThat(resumo.totalFormatado()).isEqualTo("R$ 1.250,00");
    }

    @Test
    @DisplayName("Soma nula deve resultar em zero formatado")
    void somaNulaDeveResultarEmZero() {
        ResumoFinanceiroService servico = servicoEm(LocalDateTime.of(2026, 9, 19, 10, 0));
        when(usuarioAutenticado.obter()).thenReturn(ANA);
        when(tarefaRepository.somarValorAbertasNoPeriodo("ana-id", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(null);

        ResumoFinanceiroRespostaDTO resumo = servico.obter();

        assertThat(resumo.totalAGastar()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.totalFormatado()).isEqualTo("R$ 0,00");
    }

    @Test
    @DisplayName("Fevereiro de ano bissexto deve terminar no dia 29")
    void fevereiroBissextoDeveTerminarNoDia29() {
        ResumoFinanceiroService servico = servicoEm(LocalDateTime.of(2028, 2, 10, 10, 0));
        when(usuarioAutenticado.obter()).thenReturn(ANA);
        when(tarefaRepository.somarValorAbertasNoPeriodo("ana-id", LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29)))
                .thenReturn(BigDecimal.TEN);

        ResumoFinanceiroRespostaDTO resumo = servico.obter();

        assertThat(resumo.mes()).isEqualTo(2);
        assertThat(resumo.ano()).isEqualTo(2028);
        assertThat(resumo.totalFormatado()).isEqualTo("R$ 10,00");
    }

    private ResumoFinanceiroService servicoEm(LocalDateTime agora) {
        Clock relogio = Clock.fixed(agora.atZone(FUSO).toInstant(), FUSO);
        return new ResumoFinanceiroService(tarefaRepository, usuarioAutenticado, relogio);
    }
}
