package com.task.task_backend.mapper;

import com.task.task_backend.dto.TarefaRequisicaoDTO;
import com.task.task_backend.dto.TarefaRespostaDTO;
import com.task.task_backend.entity.Situacao;
import com.task.task_backend.entity.Tarefa;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/** Conversões entre {@link Tarefa} e seus DTOs. */
@Mapper(componentModel = "spring")
public interface TarefaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuarioId", ignore = true)
    @Mapping(target = "descricaoNormalizada", ignore = true)
    @Mapping(target = "situacao", ignore = true)
    @Mapping(target = "dataAbertura", ignore = true)
    @Mapping(target = "dataConclusaoReal", ignore = true)
    @Mapping(target = "posicao", ignore = true)
    @Mapping(target = "valor", source = "valor", qualifiedByName = "normalizarValor")
    Tarefa paraEntidade(TarefaRequisicaoDTO requisicao);

    @Mapping(target = "atrasada", expression = "java(calcularAtrasada(tarefa, hoje))")
    TarefaRespostaDTO paraResposta(Tarefa tarefa, @Context LocalDate hoje);

    /** Valor ausente vira 0,00; qualquer valor é fixado em duas casas decimais. */
    @Named("normalizarValor")
    default BigDecimal normalizarValor(BigDecimal valor) {
        BigDecimal efetivo = valor == null ? BigDecimal.ZERO : valor;
        return efetivo.setScale(2, RoundingMode.HALF_UP);
    }

    /** Atrasada = aberta com prazo previsto anterior à data de hoje. */
    default boolean calcularAtrasada(Tarefa tarefa, LocalDate hoje) {
        return tarefa.getSituacao() == Situacao.ABERTA
                && tarefa.getDataPrevistaConclusao() != null
                && tarefa.getDataPrevistaConclusao().isBefore(hoje);
    }
}
