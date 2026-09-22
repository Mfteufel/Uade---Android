package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Body del POST que envía una pregunta sobre una publicación. */
public class PreguntaNuevaRequest {

    @SerializedName("texto")
    public final String texto;

    public PreguntaNuevaRequest(String texto) {
        this.texto = texto;
    }
}
