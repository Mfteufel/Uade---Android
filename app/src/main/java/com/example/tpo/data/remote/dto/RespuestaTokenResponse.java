package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Respuesta de {@code POST /auth/otp/verificar} y {@code POST /auth/login}. */
public class RespuestaTokenResponse {

    @SerializedName("token")
    public String token;

    @SerializedName("tipo")
    public String tipo;

    @SerializedName("usuario")
    public UsuarioPublicoResponse usuario;
}
