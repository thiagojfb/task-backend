package com.task.task_backend.util;

import com.task.task_backend.entity.Prioridade;
import com.task.task_backend.entity.Situacao;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formata valores para exibição no histórico (moeda brasileira, datas dd/MM/yyyy e rótulos). */
public final class FormatadorHistorico {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SEM_DATA = "—";

    private FormatadorHistorico() {
    }

    /** Ex.: 1250 → "R$ 1.250,00" (com espaço comum, não NBSP). */
    public static String formatarValor(BigDecimal valor) {
        BigDecimal efetivo = valor == null ? BigDecimal.ZERO : valor;
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
        return "R$ " + formato.format(efetivo);
    }

    public static String formatarData(LocalDate data) {
        return data == null ? SEM_DATA : FORMATO_DATA.format(data);
    }

    public static String formatarDataHora(LocalDateTime dataHora) {
        return FORMATO_DATA.format(dataHora);
    }

    public static String formatarPrioridade(Prioridade prioridade) {
        return prioridade.getRotulo();
    }

    public static String formatarSituacao(Situacao situacao) {
        return situacao.getRotulo();
    }
}
