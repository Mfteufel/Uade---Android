package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Body del POST con el que el vendedor contesta una pregunta. */
public class RespuestaNuevaRequest {

    @SerializedName("texto")
    public final String texto;

    public RespuestaNuevaRequest(String texto) {
        this.texto = texto;
    }
}
