package com.example.tpo.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Helpers de texto para la búsqueda del Home.
 */
public final class TextoUtils {

    private TextoUtils() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Deja el texto en minúsculas y sin tildes ni diéresis.
     * <p>
     * Sirve para que el buscador del Home sea tolerante: escribir "guitarra
     * electrica" tiene que encontrar "Guitarra eléctrica". Se descompone el texto
     * en forma NFD (la letra por un lado y el acento por otro) y después se borran
     * las marcas diacríticas (\p{Mn} = Mark, nonspacing).
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String descompuesto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return descompuesto.replaceAll("\\p{Mn}", "").toLowerCase(Locale.ROOT).trim();
    }
}
