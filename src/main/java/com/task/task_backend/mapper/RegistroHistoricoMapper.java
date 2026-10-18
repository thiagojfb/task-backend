package com.task.task_backend.mapper;

import com.task.task_backend.dto.RegistroHistoricoRespostaDTO;
import com.task.task_backend.entity.RegistroHistorico;
import com.task.task_backend.util.FormatadorHistorico;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Converte registros de histórico em DTOs com a frase pronta para a interface. */
@Mapper(componentModel = "spring")
public interface RegistroHistoricoMapper {

    @Mapping(target = "descricao", expression = "java(montarDescricao(registro))")
    RegistroHistoricoRespostaDTO paraResposta(RegistroHistorico registro);

    /** Ex.: "Modificado por Ana Silva em 15/09/2026 - Alterou valor de R$ 50,00 para R$ 70,00". */
    default String montarDescricao(RegistroHistorico registro) {
        String autor = registro.getUsuarioNome();
        String quando = FormatadorHistorico.formatarDataHora(registro.getDataHora());
        return switch (registro.getTipo()) {
            case CRIACAO -> "Criada por %s em %s".formatted(autor, quando);
            case ALTERACAO -> "Modificado por %s em %s - Alterou %s de %s para %s".formatted(
                    autor, quando, rotuloDoCampo(registro.getCampo()), registro.getValorAnterior(),
                    registro.getValorNovo());
            case CONCLUSAO -> "Concluída por %s em %s".formatted(autor, quando);
            case REABERTURA -> "Reaberta por %s em %s".formatted(autor, quando);
        };
    }

    /**
     * Traduz o nome técnico do campo para o rótulo em português usado na frase. Privado e estático de propósito:
     * um método {@code default String→String} seria adotado pelo MapStruct como conversão automática de {@code campo}.
     */
    private static String rotuloDoCampo(String campo) {
        return switch (campo) {
            case "descricao" -> "descrição";
            case "prioridade" -> "prioridade";
            case "dataPrevistaConclusao" -> "data prevista de conclusão";
            case "valor" -> "valor";
            default -> campo;
        };
    }
}
