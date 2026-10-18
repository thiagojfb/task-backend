package com.task.task_backend.service;

import com.task.task_backend.dto.ResumoFinanceiroRespostaDTO;
import com.task.task_backend.repository.TarefaRepository;
import com.task.task_backend.seguranca.UsuarioAutenticado;
import com.task.task_backend.util.FormatadorHistorico;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calcula o indicador "total a gastar neste mês": tarefas abertas com prazo no mês corrente (fuso de Brasília). */
@Service
@RequiredArgsConstructor
public class ResumoFinanceiroService {

    private final TarefaRepository tarefaRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public ResumoFinanceiroRespostaDTO obter() {
        String usuarioId = usuarioAutenticado.obter().id();
        YearMonth mes = YearMonth.now(relogio);
        BigDecimal soma = tarefaRepository.somarValorAbertasNoPeriodo(usuarioId, mes.atDay(1), mes.atEndOfMonth());
        BigDecimal total = (soma == null ? BigDecimal.ZERO : soma).setScale(2, RoundingMode.HALF_UP);
        return new ResumoFinanceiroRespostaDTO(mes.getMonthValue(), mes.getYear(), total,
                FormatadorHistorico.formatarValor(total));
    }
}
