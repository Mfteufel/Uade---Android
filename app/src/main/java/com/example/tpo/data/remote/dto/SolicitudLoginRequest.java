package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Cuerpo de {@code POST /auth/login}. */
public class SolicitudLoginRequest {

    @SerializedName("email")
    public final String email;

    @SerializedName("password")
    public final String password;

    public SolicitudLoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
