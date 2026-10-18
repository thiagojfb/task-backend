package com.task.task_backend.unit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.task.task_backend.dto.RegistroHistoricoRespostaDTO;
import com.task.task_backend.entity.RegistroHistorico;
import com.task.task_backend.entity.Tarefa;
import com.task.task_backend.entity.TipoRegistro;
import com.task.task_backend.mapper.RegistroHistoricoMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;

@DisplayName("RegistroHistoricoMapper")
class RegistroHistoricoMapperTest {

    private static final LocalDateTime DATA_HORA = LocalDateTime.of(2026, 9, 15, 14, 32, 10);

    private final RegistroHistoricoMapper mapper = Mappers.getMapper(RegistroHistoricoMapper.class);

    @Test
    @DisplayName("Deve copiar os campos e montar a frase de alteração")
    void deveCopiarCamposEMontarFraseDeAlteracao() {
        RegistroHistorico registro = registro(TipoRegistro.ALTERACAO, "valor", "R$ 50,00", "R$ 70,00");

        RegistroHistoricoRespostaDTO resposta = mapper.paraResposta(registro);

        assertThat(resposta.id()).isEqualTo(registro.getId());
        assertThat(resposta.usuarioNome()).isEqualTo("Ana Silva");
        assertThat(resposta.dataHora()).isEqualTo(DATA_HORA);
        assertThat(resposta.tipo()).isEqualTo(TipoRegistro.ALTERACAO);
        assertThat(resposta.campo()).isEqualTo("valor");
        assertThat(resposta.valorAnterior()).isEqualTo("R$ 50,00");
        assertThat(resposta.valorNovo()).isEqualTo("R$ 70,00");
        assertThat(resposta.descricao())
                .isEqualTo("Modificado por Ana Silva em 15/09/2026 - Alterou valor de R$ 50,00 para R$ 70,00");
    }

    @ParameterizedTest(name = "campo {0} → rótulo \"{1}\"")
    @CsvSource({
            "descricao, descrição",
            "prioridade, prioridade",
            "dataPrevistaConclusao, data prevista de conclusão",
            "valor, valor",
            "outroCampo, outroCampo"
    })
    @DisplayName("Deve traduzir o nome do campo para português na frase")
    void deveTraduzirNomeDoCampo(String campo, String rotulo) {
        RegistroHistorico registro = registro(TipoRegistro.ALTERACAO, campo, "A", "B");

        assertThat(mapper.paraResposta(registro).descricao())
                .isEqualTo("Modificado por Ana Silva em 15/09/2026 - Alterou " + rotulo + " de A para B");
    }

    @Test
    @DisplayName("Deve montar frase de criação")
    void deveMontarFraseDeCriacao() {
        RegistroHistorico registro = registro(TipoRegistro.CRIACAO, null, null, null);

        RegistroHistoricoRespostaDTO resposta = mapper.paraResposta(registro);

        assertThat(resposta.descricao()).isEqualTo("Criada por Ana Silva em 15/09/2026");
        assertThat(resposta.campo()).isNull();
    }

    @Test
    @DisplayName("Deve montar frase de conclusão")
    void deveMontarFraseDeConclusao() {
        RegistroHistorico registro = registro(TipoRegistro.CONCLUSAO, "situacao", "Aberta", "Concluída");

        assertThat(mapper.paraResposta(registro).descricao()).isEqualTo("Concluída por Ana Silva em 15/09/2026");
    }

    @Test
    @DisplayName("Deve montar frase de reabertura")
    void deveMontarFraseDeReabertura() {
        RegistroHistorico registro = registro(TipoRegistro.REABERTURA, "situacao", "Concluída", "Aberta");

        assertThat(mapper.paraResposta(registro).descricao()).isEqualTo("Reaberta por Ana Silva em 15/09/2026");
    }

    private static RegistroHistorico registro(TipoRegistro tipo, String campo, String anterior, String novo) {
        return RegistroHistorico.builder()
                .id(UUID.randomUUID())
                .tarefa(Tarefa.builder().id(UUID.randomUUID()).build())
                .usuarioId("ana-id")
                .usuarioNome("Ana Silva")
                .dataHora(DATA_HORA)
                .tipo(tipo)
                .campo(campo)
                .valorAnterior(anterior)
                .valorNovo(novo)
                .build();
    }
}
