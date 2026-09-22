package com.example.tpo.data;

import androidx.annotation.Nullable;

import com.example.tpo.model.Usuario;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validaciones de los datos editables del perfil (Punto 2).
 * <p>
 * Las usan las dos implementaciones de {@link PerfilRepository}: el backend
 * todavía no valida formato, así que la API también las necesita para no guardar
 * datos absurdos. Es Java puro (sin {@code android.util.Patterns}) para poder
 * probarla con un test unitario común.
 */
public final class ValidadorPerfil {

    public static final int LARGO_MAXIMO_NOMBRE = 60;
    public static final int MINIMO_DIGITOS_TELEFONO = 8;
    public static final int MAXIMO_DIGITOS_TELEFONO = 15;

    /** Algo@algo.algo, sin espacios. No pretende cubrir todo el RFC, solo cortar lo absurdo. */
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    /** Dígitos con los separadores habituales: "+54 11 5555-0000", "(011) 5555 0000". */
    private static final Pattern CARACTERES_TELEFONO = Pattern.compile("^\\+?[0-9 ()\\-]+$");

    private ValidadorPerfil() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Devuelve el mensaje de error, o null si está todo bien.
     * <p>
     * Devuelve String y no lanza excepción porque el mensaje va derecho al callback
     * de error, el mismo camino que usa la API cuando responde un 400.
     */
    @Nullable
    public static String validar(Usuario usuario) {
        String nombre = recortado(usuario.getNombre());
        if (nombre.isEmpty()) {
            return "Ingresá tu nombre";
        }
        if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
            return "El nombre no puede superar los " + LARGO_MAXIMO_NOMBRE + " caracteres";
        }
        String email = recortado(usuario.getEmail());
        if (email.isEmpty()) {
            return "Ingresá tu email";
        }
        if (!EMAIL.matcher(email).matches()) {
            return "El email no tiene un formato válido";
        }
        if (usuario.getZona() == null) {
            return "Elegí tu zona";
        }
        // El teléfono es opcional, pero si lo cargan tiene que ser usable: es el
        // dato con el que la otra parte coordina la entrega en mano.
        String telefono = recortado(usuario.getTelefono());
        if (!telefono.isEmpty()) {
            if (!CARACTERES_TELEFONO.matcher(telefono).matches()) {
                return "El teléfono solo puede tener números, espacios, guiones y +";
            }
            int digitos = telefono.replaceAll("[^0-9]", "").length();
            if (digitos < MINIMO_DIGITOS_TELEFONO) {
                return "El teléfono debe tener al menos " + MINIMO_DIGITOS_TELEFONO + " dígitos";
            }
            if (digitos > MAXIMO_DIGITOS_TELEFONO) {
                return "El teléfono no puede tener más de " + MAXIMO_DIGITOS_TELEFONO + " dígitos";
            }
        }
        return null;
    }

    /**
     * Copia lista para guardar: sin espacios sobrantes, email en minúsculas (dos
     * altas con distinto casing no son usuarios distintos) y teléfono vacío como null.
     */
    public static Usuario normalizar(Usuario usuario) {
        String telefono = recortado(usuario.getTelefono());
        return usuario.conDatosPersonales(
                recortado(usuario.getNombre()),
                recortado(usuario.getEmail()).toLowerCase(Locale.ROOT),
                telefono.isEmpty() ? null : telefono,
                usuario.getZona());
    }

    private static String recortado(@Nullable String texto) {
        return texto == null ? "" : texto.trim();
    }
}
