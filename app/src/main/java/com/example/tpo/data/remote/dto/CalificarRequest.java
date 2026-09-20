package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de {@code POST /operaciones/{id}/calificacion}.
 * <p>
 * No lleva a quién se califica ni quién califica: el autor sale del token y el
 * calificado es la otra parte de la operación. Así el servidor no tiene que
 * confiar en la app para eso.
 */
public class CalificarRequest {

    @SerializedName("estrellas")
    public final int estrellas;

    /** Opcional: null si no escribió nada. */
    @SerializedName("comentario")
    public final String comentario;

    public CalificarRequest(int estrellas, String comentario) {
        this.estrellas = estrellas;
        this.comentario = comentario;
    }
}
