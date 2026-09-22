package com.example.tpo.model;

import java.io.Serializable;

/**
 * Una pregunta que un interesado le hizo al vendedor sobre una publicación —
 * acción "preguntar" del Punto 4.
 * <p>
 * La trae y la envía {@link com.example.tpo.data.PreguntaRepositoryApi} contra
 * el backend real ({@code GET}/{@code POST /publicaciones/{id}/preguntas}).
 * Todos los campos son finales: una pregunta enviada no se edita, se manda otra.
 */
public class Pregunta implements Serializable {

    private final String publicacionId;
    private final String autorId;
    private final String autorNombre;
    private final String texto;
    /** Momento del envío, en milisegundos desde epoch. */
    private final long fecha;

    public Pregunta(String publicacionId, String autorId, String autorNombre, String texto, long fecha) {
        this.publicacionId = publicacionId;
        this.autorId = autorId;
        this.autorNombre = autorNombre;
        this.texto = texto;
        this.fecha = fecha;
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
}
