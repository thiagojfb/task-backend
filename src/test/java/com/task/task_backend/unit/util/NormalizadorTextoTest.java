package com.task.task_backend.unit.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.task.task_backend.util.NormalizadorTexto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@DisplayName("NormalizadorTexto")
class NormalizadorTextoTest {

    @ParameterizedTest(name = "\"{0}\" deve virar \"{1}\"")
    @CsvSource({
            "Comprar Lâmpada, comprar lampada",
            "Ação à Ç, acao a c",
            "'  Espaços  ', espacos",
            "PAGAR LUZ, pagar luz"
    })
    @DisplayName("Deve remover acentos, aparar espaços e converter para minúsculas")
    void deveNormalizarTexto(String entrada, String esperado) {
        assertThat(NormalizadorTexto.normalizar(entrada)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Deve retornar texto vazio para entrada nula ou vazia")
    void deveRetornarVazioParaEntradaNulaOuVazia(String entrada) {
        assertThat(NormalizadorTexto.normalizar(entrada)).isEmpty();
    }
}
