package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Respuesta de {@code POST /auth/otp} y {@code POST /auth/otp/reenviar}.
 * <p>
 * {@code codigo} solo viaja en desarrollo (el backend lo omite en producción);
 * la app lo ignora, no lo autocompleta en ningún lado.
 */
public class RespuestaOtpResponse {

    @SerializedName("mensaje")
    public String mensaje;

    @SerializedName("expira_en_segundos")
    public int expiraEnSegundos;

    @SerializedName("codigo")
    public String codigo;
}
