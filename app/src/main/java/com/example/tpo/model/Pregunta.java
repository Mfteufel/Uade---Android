package com.example.tpo.model;

import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Una pregunta que un interesado le hizo al vendedor sobre una publicación —
 * acción "preguntar" del Punto 4. Es pública: cualquiera que mira la
 * publicación ve todas las preguntas (con quién las hizo) y, si el vendedor ya
 * la contestó, también la respuesta — mismo criterio que cualquier
 * marketplace (MercadoLibre incluido).
 * <p>
 * La trae y la envía {@link com.example.tpo.data.PreguntaRepositoryApi} contra
 * el backend real ({@code GET}/{@code POST /publicaciones/{id}/preguntas},
 * {@code POST .../preguntas/{id}/respuesta}). El texto de la pregunta no se
 * edita (se manda otra), pero la respuesta sí puede llegar después: por eso
 * {@link #respuesta}/{@link #respuestaFecha} no son finales.
 */
public class Pregunta implements Serializable {

    private final String id;
    private final String publicacionId;
    private final String autorId;
    private final String autorNombre;
    private final String texto;
    /** Momento del envío, en milisegundos desde epoch. */
    private final long fecha;

    @Nullable
    private String respuesta;
    /** Momento en que el vendedor contestó, o {@code null} si todavía no lo hizo. */
    @Nullable
    private Long respuestaFecha;

    public Pregunta(String id, String publicacionId, String autorId, String autorNombre,
                    String texto, long fecha, @Nullable String respuesta, @Nullable Long respuestaFecha) {
        this.id = id;
        this.publicacionId = publicacionId;
        this.autorId = autorId;
        this.autorNombre = autorNombre;
        this.texto = texto;
        this.fecha = fecha;
        this.respuesta = respuesta;
        this.respuestaFecha = respuestaFecha;
    }

    public String getId() {
        return id;
    }

    public String getPublicacionId() {
        return publicacionId;
    }

    public String getAutorId() {
        return autorId;
    }

    public String getAutorNombre() {
        return autorNombre;
    }

    public String getTexto() {
        return texto;
    }

    public long getFecha() {
        return fecha;
    }

    @Nullable
    public String getRespuesta() {
        return respuesta;
    }

    @Nullable
    public Long getRespuestaFecha() {
        return respuestaFecha;
    }

    public boolean tieneRespuesta() {
        return respuesta != null;
    }
}
