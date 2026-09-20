package com.example.tpo.data.remote;

import com.example.tpo.data.remote.dto.ActualizarPerfilRequest;
import com.example.tpo.data.remote.dto.CalificacionResponse;
import com.example.tpo.data.remote.dto.UsuarioResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Url;

/**
 * Endpoints de usuarios, perfil y reputación (Punto 2) según
 * {@code docs/contrato-api-perfil-historial.md}.
 * <p>
 * Retrofit la implementa a partir del único {@code Retrofit} de
 * {@code login/NetworkModule}, que ya agrega {@code Authorization: Bearer} en
 * cada request. Ningún Fragment la conoce: la usa {@code PerfilRepositoryApi}.
 */
public interface UsuarioApi {

    @GET("usuarios/me")
    Call<UsuarioResponse> obtenerMiPerfil();

    @PATCH("usuarios/me")
    Call<UsuarioResponse> actualizarMiPerfil(@Body ActualizarPerfilRequest datos);

    /** Multipart con un único campo {@code foto} (JPEG). Responde el perfil actualizado. */
    @Multipart
    @PUT("usuarios/me/foto")
    Call<UsuarioResponse> subirFoto(@Part MultipartBody.Part foto);

    @GET("usuarios/{id}")
    Call<UsuarioResponse> obtenerPerfilPublico(@Path("id") String usuarioId);

    /**
     * Descarga una imagen a partir de la {@code foto_url} que vino en el perfil
     * (relativa a la URL base). Se usa {@code @Url} para no atar la app a una ruta
     * fija: si el backend decide servir las fotos desde otro lado, no cambia nada.
     */
    @GET
    Call<ResponseBody> descargar(@Url String url);

    @GET("usuarios/{id}/calificaciones")
    Call<List<CalificacionResponse>> obtenerCalificacionesRecibidas(@Path("id") String usuarioId);
}
