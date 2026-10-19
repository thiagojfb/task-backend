package com.task.task_backend.unit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.task.task_backend.dto.TarefaRequisicaoDTO;
import com.task.task_backend.dto.TarefaRespostaDTO;
import com.task.task_backend.entity.Prioridade;
import com.task.task_backend.entity.Situacao;
import com.task.task_backend.entity.Tarefa;
import com.task.task_backend.mapper.TarefaMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("TarefaMapper")
class TarefaMapperTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 19);

    private final TarefaMapper mapper = Mappers.getMapper(TarefaMapper.class);

    @Test
    @DisplayName("Deve copiar todos os campos para a resposta")
    void deveCopiarTodosOsCamposParaResposta() {
        Tarefa tarefa = tarefaBase().build();

        TarefaRespostaDTO resposta = mapper.paraResposta(tarefa, HOJE);

        assertThat(resposta.id()).isEqualTo(tarefa.getId());
        assertThat(resposta.descricao()).isEqualTo("Pagar conta de luz");
        assertThat(resposta.prioridade()).isEqualTo(Prioridade.ALTA);
        assertThat(resposta.situacao()).isEqualTo(Situacao.ABERTA);
        assertThat(resposta.dataAbertura()).isEqualTo(tarefa.getDataAbertura());
        assertThat(resposta.dataPrevistaConclusao()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(resposta.dataConclusaoReal()).isNull();
        assertThat(resposta.valor()).isEqualByComparingTo("250.00");
        assertThat(resposta.posicao()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve marcar como atrasada quando aberta e com prazo no passado")
    void deveMarcarAtrasadaQuandoAbertaComPrazoNoPassado() {
        Tarefa tarefa = tarefaBase().dataPrevistaConclusao(HOJE.minusDays(1)).build();

        assertThat(mapper.paraResposta(tarefa, HOJE).atrasada()).isTrue();
    }

    @Test
    @DisplayName("Não deve marcar como atrasada quando concluída, mesmo com prazo no passado")
    void naoDeveMarcarAtrasadaQuandoConcluida() {
        Tarefa tarefa = tarefaBase()
                .situacao(Situacao.CONCLUIDA)
                .dataPrevistaConclusao(HOJE.minusDays(5))
                .build();

        assertThat(mapper.paraResposta(tarefa, HOJE).atrasada()).isFalse();
    }

    @Test
    @DisplayName("Não deve marcar como atrasada quando não houver prazo")
    void naoDeveMarcarAtrasadaSemPrazo() {
        Tarefa tarefa = tarefaBase().dataPrevistaConclusao(null).build();

        assertThat(mapper.paraResposta(tarefa, HOJE).atrasada()).isFalse();
    }

    @Test
    @DisplayName("Não deve marcar como atrasada quando o prazo for hoje")
    void naoDeveMarcarAtrasadaQuandoPrazoForHoje() {
        Tarefa tarefa = tarefaBase().dataPrevistaConclusao(HOJE).build();

        assertThat(mapper.paraResposta(tarefa, HOJE).atrasada()).isFalse();
    }

    @Test
    @DisplayName("Deve converter requisição em entidade ignorando campos não editáveis")
    void deveConverterRequisicaoEmEntidadeIgnorandoCamposNaoEditaveis() {
        TarefaRequisicaoDTO requisicao = new TarefaRequisicaoDTO(
                "Comprar lâmpada", Prioridade.MEDIA, LocalDate.of(2026, 10, 1), new BigDecimal("45.9"));

        Tarefa entidade = mapper.paraEntidade(requisicao);

        assertThat(entidade.getDescricao()).isEqualTo("Comprar lâmpada");
        assertThat(entidade.getPrioridade()).isEqualTo(Prioridade.MEDIA);
        assertThat(entidade.getDataPrevistaConclusao()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(entidade.getValor()).isEqualTo(new BigDecimal("45.90"));
        assertThat(entidade.getId()).isNull();
        assertThat(entidade.getUsuarioId()).isNull();
        assertThat(entidade.getPosicao()).isNull();
        assertThat(entidade.getDataAbertura()).isNull();
        assertThat(entidade.getSituacao()).isNull();
        assertThat(entidade.getDataConclusaoReal()).isNull();
    }

    @Test
    @DisplayName("Deve converter valor nulo em zero com duas casas")
    void deveConverterValorNuloEmZero() {
        TarefaRequisicaoDTO requisicao = new TarefaRequisicaoDTO("Ler livro", Prioridade.BAIXA, null, null);

        assertThat(mapper.paraEntidade(requisicao).getValor()).isEqualTo(new BigDecimal("0.00"));
    }

    private static Tarefa.TarefaBuilder tarefaBase() {
        return Tarefa.builder()
                .id(UUID.randomUUID())
                .usuarioId("ana-id")
                .descricao("Pagar conta de luz")
                .prioridade(Prioridade.ALTA)
                .situacao(Situacao.ABERTA)
                .dataAbertura(LocalDateTime.of(2026, 9, 19, 10, 15, 30))
                .dataPrevistaConclusao(LocalDate.of(2026, 9, 30))
                .valor(new BigDecimal("250.00"))
                .posicao(1);
    }
}
