package com.task.task_backend.unit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.task.task_backend.entity.Prioridade;
import com.task.task_backend.entity.RegistroHistorico;
import com.task.task_backend.entity.Situacao;
import com.task.task_backend.entity.Tarefa;
import com.task.task_backend.entity.TipoRegistro;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Entidades Tarefa e RegistroHistorico")
class TarefaTest {

    @Test
    @DisplayName("Ao persistir deve normalizar a descrição e aplicar padrões")
    void aoPersistirDeveNormalizarDescricaoEAplicarPadroes() {
        Tarefa tarefa = Tarefa.builder()
                .descricao("Comprar Lâmpada")
                .prioridade(Prioridade.MEDIA)
                .build();

        tarefa.aoPersistir();

        assertThat(tarefa.getDescricaoNormalizada()).isEqualTo("comprar lampada");
        assertThat(tarefa.getSituacao()).isEqualTo(Situacao.ABERTA);
        assertThat(tarefa.getValor()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Ao persistir não deve sobrescrever situação e valor já informados")
    void aoPersistirNaoDeveSobrescreverSituacaoEValorInformados() {
        Tarefa tarefa = Tarefa.builder()
                .descricao("Pagar luz")
                .situacao(Situacao.CONCLUIDA)
                .valor(new BigDecimal("12.34"))
                .build();

        tarefa.aoPersistir();

        assertThat(tarefa.getSituacao()).isEqualTo(Situacao.CONCLUIDA);
        assertThat(tarefa.getValor()).isEqualByComparingTo("12.34");
    }

    @Test
    @DisplayName("Ao atualizar deve renormalizar a descrição")
    void aoAtualizarDeveRenormalizarDescricao() {
        Tarefa tarefa = Tarefa.builder().descricao("Antiga").build();
        tarefa.aoPersistir();

        tarefa.setDescricao("Nova Descrição");
        tarefa.aoAtualizar();

        assertThat(tarefa.getDescricaoNormalizada()).isEqualTo("nova descricao");
    }

    @Test
    @DisplayName("Igualdade deve considerar apenas o identificador")
    void igualdadeDeveConsiderarApenasIdentificador() {
        UUID id = UUID.randomUUID();
        Tarefa primeira = Tarefa.builder().id(id).descricao("A").build();
        Tarefa segunda = Tarefa.builder().id(id).descricao("B").build();
        Tarefa terceira = Tarefa.builder().id(UUID.randomUUID()).descricao("A").build();

        assertThat(primeira).isEqualTo(segunda).hasSameHashCodeAs(segunda);
        assertThat(primeira).isNotEqualTo(terceira);
    }

    @Test
    @DisplayName("RegistroHistorico construído via builder deve manter todos os campos")
    void registroHistoricoDeveManterTodosOsCampos() {
        Tarefa tarefa = Tarefa.builder().id(UUID.randomUUID()).build();
        LocalDateTime agora = LocalDateTime.of(2026, 9, 15, 10, 0);

        RegistroHistorico registro = RegistroHistorico.builder()
                .id(UUID.randomUUID())
                .tarefa(tarefa)
                .usuarioId("ana-id")
                .usuarioNome("Ana Silva")
                .dataHora(agora)
                .tipo(TipoRegistro.ALTERACAO)
                .campo("valor")
                .valorAnterior("R$ 50,00")
                .valorNovo("R$ 70,00")
                .build();

        assertThat(registro.getTarefa()).isSameAs(tarefa);
        assertThat(registro.getUsuarioId()).isEqualTo("ana-id");
        assertThat(registro.getUsuarioNome()).isEqualTo("Ana Silva");
        assertThat(registro.getDataHora()).isEqualTo(agora);
        assertThat(registro.getTipo()).isEqualTo(TipoRegistro.ALTERACAO);
        assertThat(registro.getCampo()).isEqualTo("valor");
        assertThat(registro.getValorAnterior()).isEqualTo("R$ 50,00");
        assertThat(registro.getValorNovo()).isEqualTo("R$ 70,00");
    }
}
