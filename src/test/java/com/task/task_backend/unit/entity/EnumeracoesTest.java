package com.task.task_backend.unit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.task.task_backend.entity.DestinoMovimento;
import com.task.task_backend.entity.FiltroSituacao;
import com.task.task_backend.entity.Prioridade;
import com.task.task_backend.entity.Situacao;
import com.task.task_backend.entity.TipoRegistro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Enumerações do domínio")
class EnumeracoesTest {

    @Test
    @DisplayName("Prioridade deve expor rótulos em português")
    void prioridadeDeveExporRotulosEmPortugues() {
        assertThat(Prioridade.ALTA.getRotulo()).isEqualTo("Alta");
        assertThat(Prioridade.MEDIA.getRotulo()).isEqualTo("Média");
        assertThat(Prioridade.BAIXA.getRotulo()).isEqualTo("Baixa");
    }

    @Test
    @DisplayName("Situação deve expor rótulos em português")
    void situacaoDeveExporRotulosEmPortugues() {
        assertThat(Situacao.ABERTA.getRotulo()).isEqualTo("Aberta");
        assertThat(Situacao.CONCLUIDA.getRotulo()).isEqualTo("Concluída");
    }

    @Test
    @DisplayName("FiltroSituacao deve converter para situação ou vazio")
    void filtroSituacaoDeveConverterParaSituacao() {
        assertThat(FiltroSituacao.TODAS.paraSituacao()).isEmpty();
        assertThat(FiltroSituacao.ABERTAS.paraSituacao()).contains(Situacao.ABERTA);
        assertThat(FiltroSituacao.CONCLUIDAS.paraSituacao()).contains(Situacao.CONCLUIDA);
    }

    @Test
    @DisplayName("TipoRegistro e DestinoMovimento devem ter exatamente os valores do contrato")
    void tipoRegistroEDestinoMovimentoDevemTerValoresDoContrato() {
        assertThat(TipoRegistro.values())
                .extracting(Enum::name)
                .containsExactly("CRIACAO", "ALTERACAO", "CONCLUSAO", "REABERTURA");
        assertThat(DestinoMovimento.values())
                .extracting(Enum::name)
                .containsExactly("TOPO", "FINAL", "POSICAO");
    }
}
