package com.example.tpo.data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Publicaciones que el usuario marcó como guardadas, en memoria.
 * <p>
 * Simplificación acordada para las primeras entregas, igual que
 * {@link SesionUsuario}: el estado vive en un singleton y se pierde al cerrar la
 * app. Cuando la cátedra vea {@code SharedPreferences} (o {@code Room}) hay que
 * persistir esto de verdad; contra la API real sería un {@code POST/DELETE
 * /publicaciones/{id}/guardada}.
 * <p>
 * Se modela aparte de {@link SesionUsuario} a propósito: esa clase modela
 * <em>quién</em> es el usuario (el Punto 1 la va a llenar con email y token),
 * mientras que las guardadas son <em>datos</em> suyos. Tenerlas separadas hace
 * que el reemplazo futuro sea un archivo nuevo y no una cirugía.
 */
public class PublicacionesGuardadas {

    private static PublicacionesGuardadas instancia;

    /**
     * Ids de las publicaciones guardadas. {@link LinkedHashSet} y no
     * {@code HashSet} para conservar el orden en que se guardaron: la futura
     * pantalla "Guardadas" las va a querer mostrar con la última arriba.
     */
    private final Set<String> ids = new LinkedHashSet<>();

    private PublicacionesGuardadas() {
        // Constructor privado: se accede siempre por getInstancia().
    }

    public static synchronized PublicacionesGuardadas getInstancia() {
        if (instancia == null) {
            instancia = new PublicacionesGuardadas();
        }
        return instancia;
    }

    public boolean estaGuardada(String publicacionId) {
        return ids.contains(publicacionId);
    }

    /**
     * Alterna el estado de una publicación y devuelve el nuevo:
     * {@code true} si quedó guardada, {@code false} si se quitó.
     */
    public boolean alternar(String publicacionId) {
        if (ids.remove(publicacionId)) {
            return false;
        }
        ids.add(publicacionId);
        return true;
    }

    public int cantidad() {
        return ids.size();
    }

    /** Copia defensiva: nadie modifica el set interno desde afuera. */
    public Set<String> getIds() {
        return new LinkedHashSet<>(ids);
    }
}
