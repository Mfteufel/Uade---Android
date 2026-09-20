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

    /**
     * Iniciales para el avatar del vendedor: primera letra del primer nombre y
     * del último. "Martina G." → "MG", "Escuela de Música" → "EM",
     * "Laura y Seba" → "LS", "Bruno" → "B". Devuelve "?" si el nombre viene vacío.
     */
    public static String iniciales(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder resultado = new StringBuilder();
        agregarPrimeraLetra(resultado, partes[0]);
        if (partes.length > 1) {
            agregarPrimeraLetra(resultado, partes[partes.length - 1]);
        }
        return resultado.length() == 0 ? "?" : resultado.toString();
    }

    /** Agrega en mayúscula la primera letra (Character.isLetter) de {@code palabra}, si tiene alguna. */
    private static void agregarPrimeraLetra(StringBuilder destino, String palabra) {
        for (int i = 0; i < palabra.length(); i++) {
            char c = palabra.charAt(i);
            if (Character.isLetter(c)) {
                destino.append(Character.toUpperCase(c));
                return;
            }
        }
    }
}
