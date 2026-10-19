package com.task.task_backend.unit.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.task.task_backend.entity.Prioridade;
import com.task.task_backend.entity.Situacao;
import com.task.task_backend.util.FormatadorHistorico;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FormatadorHistorico")
class FormatadorHistoricoTest {

    @Test
    @DisplayName("Deve formatar valor com separador de milhar, vírgula decimal e espaço comum")
    void deveFormatarValorComMilharEVirgula() {
        String formatado = FormatadorHistorico.formatarValor(new BigDecimal("1250"));

        assertThat(formatado).isEqualTo("R$ 1.250,00");
        assertThat(formatado.charAt(2)).isEqualTo(' ');
    }

    @Test
    @DisplayName("Deve formatar valor com duas casas decimais")
    void deveFormatarValorComDuasCasas() {
        assertThat(FormatadorHistorico.formatarValor(new BigDecimal("50.5"))).isEqualTo("R$ 50,50");
    }

    @Test
    @DisplayName("Deve formatar valor nulo como zero")
    void deveFormatarValorNuloComoZero() {
        assertThat(FormatadorHistorico.formatarValor(null)).isEqualTo("R$ 0,00");
    }

    @Test
    @DisplayName("Deve formatar data no padrão brasileiro")
    void deveFormatarDataNoPadraoBrasileiro() {
        assertThat(FormatadorHistorico.formatarData(LocalDate.of(2026, 9, 30))).isEqualTo("30/09/2026");
    }

    @Test
    @DisplayName("Deve formatar data nula como travessão")
    void deveFormatarDataNulaComoTravessao() {
        assertThat(FormatadorHistorico.formatarData(null)).isEqualTo("—");
    }

    @Test
    @DisplayName("Deve formatar data e hora exibindo apenas a data")
    void deveFormatarDataHoraExibindoApenasData() {
        assertThat(FormatadorHistorico.formatarDataHora(LocalDateTime.of(2026, 9, 15, 14, 32, 10)))
                .isEqualTo("15/09/2026");
    }

    @Test
    @DisplayName("Deve formatar prioridade e situação pelos rótulos")
    void deveFormatarPrioridadeESituacaoPelosRotulos() {
        assertThat(FormatadorHistorico.formatarPrioridade(Prioridade.ALTA)).isEqualTo("Alta");
        assertThat(FormatadorHistorico.formatarSituacao(Situacao.CONCLUIDA)).isEqualTo("Concluída");
    }
}
