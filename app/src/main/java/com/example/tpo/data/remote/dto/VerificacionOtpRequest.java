package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Cuerpo de {@code POST /auth/otp/verificar}. */
public class VerificacionOtpRequest {

    @SerializedName("email")
    public final String email;

    @SerializedName("codigo")
    public final String codigo;

    public VerificacionOtpRequest(String email, String codigo) {
        this.email = email;
        this.codigo = codigo;
    }
}
