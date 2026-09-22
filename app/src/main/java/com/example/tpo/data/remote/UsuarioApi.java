package com.example.tpo.data.remote;

import com.example.tpo.data.remote.dto.ActualizarPerfilRequest;
import com.example.tpo.data.remote.dto.CalificacionResponse;
import com.example.tpo.data.remote.dto.PaginaPublicacionesResponse;
import com.example.tpo.data.remote.dto.UsuarioResponse;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * Endpoints de usuarios, perfil y reputación (Punto 2), tal como están en el
 * backend desplegado.
 * <p>
 * Retrofit la implementa a partir del único {@code Retrofit} de
 * {@code login/NetworkModule}, que ya agrega {@code Authorization: Bearer} en
 * cada request. Ningún Fragment la conoce: la usa {@code PerfilRepositoryApi}.
 * <p>
 * El backend todavía no tiene endpoint de foto de perfil: por eso no se declara
 * acá, así ninguna pantalla puede disparar un pedido que sabemos que responde 404.
 */
public interface UsuarioApi {

    @GET("usuarios/yo")
    Call<UsuarioResponse> obtenerMiPerfil();

    /**
     * Reemplaza los cuatro datos editables. Es un PUT (actualización completa): un
     * campo que no viaja queda en null del otro lado.
     */
    @PUT("usuarios/yo")
    Call<UsuarioResponse> actualizarMiPerfil(@Body ActualizarPerfilRequest datos);

    @GET("usuarios/{id}")
    Call<UsuarioResponse> obtenerPerfilPublico(@Path("id") String usuarioId);

    /** Calificaciones que recibió el usuario, más recientes primero (Punto 9). */
    @GET("usuarios/{id}/calificaciones")
    Call<List<CalificacionResponse>> obtenerCalificacionesRecibidas(@Path("id") String usuarioId);

    /**
     * Publicaciones de un vendedor, para su perfil público. Es el mismo listado
     * paginado del Home filtrado por {@code vendedorId}; un vendedor sin
     * publicaciones responde 200 con la lista vacía.
     *
     * @param pagina empieza en 0.
     */
    @GET("publicaciones")
    Call<PaginaPublicacionesResponse> obtenerPublicacionesDeVendedor(
            @Query("vendedorId") String vendedorId,
            @Query("pagina") int pagina);

    /**
     * Descarga una imagen a partir de la {@code foto_url} que vino en el perfil
     * (relativa a la URL base). Se usa {@code @Url} para no atar la app a una ruta
     * fija. Hoy el backend no manda {@code foto_url}, así que no se llega a usar.
     */
    @GET
    Call<ResponseBody> descargar(@Url String url);
}
