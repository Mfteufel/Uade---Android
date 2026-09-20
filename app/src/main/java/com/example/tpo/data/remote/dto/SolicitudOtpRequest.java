package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Cuerpo de {@code POST /auth/otp} y {@code POST /auth/otp/reenviar}. */
public class SolicitudOtpRequest {

    @SerializedName("email")
    public final String email;

    public SolicitudOtpRequest(String email) {
        this.email = email;
    }
}
