package com.example.tpo.data;

import androidx.annotation.NonNull;

import com.example.tpo.data.remote.AuthApi;
import com.example.tpo.data.remote.ErrorApi;
import com.example.tpo.data.remote.dto.RespuestaOtpResponse;
import com.example.tpo.data.remote.dto.RespuestaTokenResponse;
import com.example.tpo.data.remote.dto.SolicitudLoginRequest;
import com.example.tpo.data.remote.dto.SolicitudOtpRequest;
import com.example.tpo.data.remote.dto.UsuarioPublicoResponse;
import com.example.tpo.data.remote.dto.VerificacionOtpRequest;
import com.example.tpo.login.TokenManager;

import java.util.function.Function;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Implementación de {@link AuthRepository} contra la API REST, con Retrofit.
 * <p>
 * No construye Retrofit: recibe {@link AuthApi} ya creada desde el único
 * {@code Retrofit} de {@code NetworkModule}.
 */
public class AuthRepositoryApi implements AuthRepository {

    private final AuthApi api;
    private final TokenManager tokenManager;

    public AuthRepositoryApi(AuthApi api, TokenManager tokenManager) {
        this.api = api;
        this.tokenManager = tokenManager;
    }

    @Override
    public void solicitarCodigo(String email, RepositorioCallback<String> callback) {
        api.solicitarCodigo(new SolicitudOtpRequest(email)).enqueue(
                adaptar(callback, "No pudimos enviar el código", respuesta -> respuesta.mensaje));
    }

    @Override
    public void reenviarCodigo(String email, RepositorioCallback<String> callback) {
        api.reenviarCodigo(new SolicitudOtpRequest(email)).enqueue(
                adaptar(callback, "No pudimos reenviar el código", respuesta -> respuesta.mensaje));
    }

    @Override
    public void verificarCodigo(String email, String codigo, RepositorioCallback<Void> callback) {
        api.verificarCodigo(new VerificacionOtpRequest(email, codigo)).enqueue(
                adaptar(callback, "El código es incorrecto o ya venció", this::guardarSesion));
    }

    @Override
    public void iniciarSesionConPassword(String email, String password, RepositorioCallback<Void> callback) {
        api.iniciarSesion(new SolicitudLoginRequest(email, password)).enqueue(
                adaptar(callback, "Email o contraseña incorrectos", this::guardarSesion));
    }

    @Override
    public void restaurarSesion(RepositorioCallback<Void> callback) {
        api.verSesion().enqueue(
                adaptar(callback, "Tu sesión venció. Volvé a iniciar sesión.", usuario -> {
                    hidratarSesion(usuario);
                    return null;
                }));
    }

    // ---------------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------------

    /** Guarda el token nuevo e hidrata la sesión con el usuario que vino junto. */
    private Void guardarSesion(RespuestaTokenResponse respuesta) {
        tokenManager.saveToken(respuesta.token);
        hidratarSesion(respuesta.usuario);
        return null;
    }

    /**
     * Vuelca la identidad del backend en {@link SesionUsuario}. También espeja
     * {@code idUsuario = usuarioId}, mismo workaround que ya hacía a mano
     * {@code LoginFragment}: son dos catálogos de id sin reconciliar todavía
     * (ver el TODO en {@link SesionUsuario#getIdUsuario()}).
     */
    private void hidratarSesion(UsuarioPublicoResponse usuario) {
        SesionUsuario sesion = SesionUsuario.getInstancia();
        sesion.setUsuarioId(usuario.id);
        sesion.setNombre(usuario.nombre);
        if (usuario.zonaModelo() != null) {
            sesion.setZona(usuario.zonaModelo());
        }
        sesion.setIdUsuario(usuario.id);
    }

    /**
     * Traduce el {@link Callback} de Retrofit al {@link RepositorioCallback} de la
     * app. A diferencia del resto de los repositorios, usa
     * {@link ErrorApi#detalle} y no {@link ErrorApi#mensaje}: acá un 401 significa
     * "código o contraseña incorrectos", nunca "sesión vencida" — todavía no hay
     * ninguna sesión que pueda haber vencido.
     */
    private static <T, R> Callback<T> adaptar(RepositorioCallback<R> callback,
                                              String errorPorDefecto,
                                              Function<T, R> aModelo) {
        return new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> llamada, @NonNull Response<T> respuesta) {
                T cuerpo = respuesta.body();
                if (!respuesta.isSuccessful() || cuerpo == null) {
                    String detalle = ErrorApi.detalle(respuesta);
                    callback.onError(detalle != null ? detalle : errorPorDefecto);
                    return;
                }
                R modelo;
                try {
                    modelo = aModelo.apply(cuerpo);
                } catch (RuntimeException excepcion) {
                    callback.onError(errorPorDefecto);
                    return;
                }
                callback.onExito(modelo);
            }

            @Override
            public void onFailure(@NonNull Call<T> llamada, @NonNull Throwable error) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        };
    }
}
