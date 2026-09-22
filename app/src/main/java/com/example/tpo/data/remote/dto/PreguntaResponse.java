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

    /** {@code null} si el vendedor todavía no la contestó. */
    @SerializedName("respuesta")
    public String respuesta;

    @SerializedName("respuestaFecha")
    public Long respuestaFecha;

    public Pregunta aModelo() {
        return new Pregunta(id, publicacionId, autorId, autorNombre, texto, fecha, respuesta, respuestaFecha);
    }
}
