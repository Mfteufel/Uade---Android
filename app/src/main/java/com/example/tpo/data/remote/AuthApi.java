package com.example.tpo.data.remote;

import com.example.tpo.data.remote.dto.RespuestaOtpResponse;
import com.example.tpo.data.remote.dto.RespuestaTokenResponse;
import com.example.tpo.data.remote.dto.SolicitudLoginRequest;
import com.example.tpo.data.remote.dto.SolicitudOtpRequest;
import com.example.tpo.data.remote.dto.UsuarioPublicoResponse;
import com.example.tpo.data.remote.dto.VerificacionOtpRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Endpoints de autenticación (Punto 1) contra {@code backend/routers/auth.py}.
 * <p>
 * La usa {@code AuthRepositoryApi}. {@code verSesion} no lleva token explícito
 * en la firma: lo agrega el interceptor de {@code NetworkModule} si hay uno
 * guardado en {@code TokenManager}.
 */
public interface AuthApi {

    @POST("auth/otp")
    Call<RespuestaOtpResponse> solicitarCodigo(@Body SolicitudOtpRequest datos);

    @POST("auth/otp/reenviar")
    Call<RespuestaOtpResponse> reenviarCodigo(@Body SolicitudOtpRequest datos);

    @POST("auth/otp/verificar")
    Call<RespuestaTokenResponse> verificarCodigo(@Body VerificacionOtpRequest datos);

    @POST("auth/login")
    Call<RespuestaTokenResponse> iniciarSesion(@Body SolicitudLoginRequest datos);

    @GET("auth/sesion")
    Call<UsuarioPublicoResponse> verSesion();
}
