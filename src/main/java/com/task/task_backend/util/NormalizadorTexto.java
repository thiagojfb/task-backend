package com.task.task_backend.util;

import java.text.Normalizer;
import java.util.Locale;

/** Normaliza textos para busca: remove acentos, apara espaços e converte para minúsculas. */
public final class NormalizadorTexto {

    private NormalizadorTexto() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcentos.trim().toLowerCase(Locale.ROOT);
    }
}
