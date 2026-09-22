package com.example.tpo.data.remote.dto;

import com.example.tpo.model.Pregunta;
import com.google.gson.annotations.SerializedName;

/** JSON de una pregunta, tal como la devuelve el backend. */
public class PreguntaResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("publicacionId")
    public String publicacionId;

    @SerializedName("autorId")
    public String autorId;

    @SerializedName("autorNombre")
    public String autorNombre;

    @SerializedName("texto")
    public String texto;

    @SerializedName("fecha")
    public long fecha;

    public Pregunta aModelo() {
        return new Pregunta(publicacionId, autorId, autorNombre, texto, fecha);
    }
}
