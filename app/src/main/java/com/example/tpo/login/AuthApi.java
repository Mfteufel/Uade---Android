package com.example.tpo.login;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("auth/login")
    Call<SesionResponse> login(@Body LoginRequest body);

    @POST("auth/otp")
    Call<CodigoResponse> pedirCodigo(@Body CodigoRequest body);

    @POST("auth/otp/reenviar")
    Call<CodigoResponse> reenviarCodigo(@Body CodigoRequest body);

    @POST("auth/otp/verificar")
    Call<SesionResponse> verificarCodigo(@Body CodigoRequest body);

    @GET("auth/sesion")
    Call<SesionResponse.Usuario> sesionActual();
}
