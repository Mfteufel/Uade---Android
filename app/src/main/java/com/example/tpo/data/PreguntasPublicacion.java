package com.example.tpo.data;

import com.example.tpo.model.Pregunta;

import java.util.ArrayList;
import java.util.List;

/**
 * Preguntas que los interesados le hicieron a los vendedores, en memoria.
 * <p>
 * Misma simplificación que {@link PublicacionesGuardadas} y
 * {@link SesionUsuario}: vive en un singleton y se pierde al cerrar la app.
 * Cuando la cátedra vea {@code SharedPreferences} o {@code Room} hay que
 * persistir esto de verdad; contra la API real cada pregunta sale de un
 * {@code POST /publicaciones/{id}/preguntas}.
 * <p>
 * A diferencia de {@link OfertasPublicacion}, acá no hay límite: un mismo
 * usuario puede mandar varias preguntas sobre la misma publicación, es lo
 * normal en una conversación.
 */
public class PreguntasPublicacion {

    private static PreguntasPublicacion instancia;

    private final List<Pregunta> preguntas = new ArrayList<>();

    private PreguntasPublicacion() {
        // Constructor privado: se accede siempre por getInstancia().
    }

    public static synchronized PreguntasPublicacion getInstancia() {
        if (instancia == null) {
            instancia = new PreguntasPublicacion();
        }
        return instancia;
    }

    public void agregar(Pregunta pregunta) {
        preguntas.add(pregunta);
    }

    /** Todas las preguntas de una publicación, para que las vea su vendedor en la gestión. */
    public List<Pregunta> deLaPublicacion(String publicacionId) {
        List<Pregunta> resultado = new ArrayList<>();
        for (Pregunta pregunta : preguntas) {
            if (pregunta.getPublicacionId().equals(publicacionId)) {
                resultado.add(pregunta);
            }
        }
        return resultado;
    }

    /** Solo las preguntas de un usuario puntual, para que el Detalle muestre "lo que enviaste". */
    public List<Pregunta> delUsuario(String publicacionId, String usuarioId) {
        List<Pregunta> resultado = new ArrayList<>();
        for (Pregunta pregunta : preguntas) {
            if (pregunta.getPublicacionId().equals(publicacionId)
                    && pregunta.getAutorId().equals(usuarioId)) {
                resultado.add(pregunta);
            }
        }
        return resultado;
    }

    public int cantidadEn(String publicacionId) {
        return deLaPublicacion(publicacionId).size();
    }
}
