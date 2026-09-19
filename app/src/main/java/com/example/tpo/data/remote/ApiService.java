package com.example.tpo.data.remote;

import com.example.tpo.data.remote.dto.CambiarEstadoPublicacionRequest;
import com.example.tpo.data.remote.dto.MiPublicacionResponse;
import com.example.tpo.data.remote.dto.PublicacionCreadaResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Contrato con la API_Rest del TPO para el Punto 5 (Publicar artículo).
 * <p>
 * Todavía no existe un backend real: {@link ApiClient#BASE_URL} apunta a un
 * placeholder. Esta interfaz queda lista para que el día que haya una URL de
 * verdad el único cambio sea esa constante (y, si los nombres de campo no
 * coinciden, los DTOs de {@code data.remote.dto}); ni el ViewModel ni las
 * pantallas del wizard dependen de Retrofit directamente.
 */
public interface ApiService {

    /**
     * Crea una publicación. Es {@code Multipart} porque manda archivos (las
     * fotos) junto con el resto de los campos en el mismo request.
     */
    @Multipart
    @POST("publicaciones")
    Call<PublicacionCreadaResponse> crearPublicacion(
            @Part("vendedorId") RequestBody vendedorId,
            @Part("titulo") RequestBody titulo,
            @Part("descripcion") RequestBody descripcion,
            @Part("categoria") RequestBody categoria,
            @Part("estadoArticulo") RequestBody estadoArticulo,
            @Part("precio") RequestBody precio,
            @Part("zona") RequestBody zona,
            @Part List<MultipartBody.Part> fotos);

    /** Publicaciones del vendedor logueado, para la pantalla "Mis publicaciones". */
    @GET("publicaciones/mias")
    Call<List<MiPublicacionResponse>> listarMisPublicaciones(@Query("vendedorId") String vendedorId);

    /** Pausa o reactiva una publicación (nunca la marca vendida desde acá). */
    @PATCH("publicaciones/{id}/estado")
    Call<Void> cambiarEstadoPublicacion(@Path("id") String id,
                                        @Body CambiarEstadoPublicacionRequest request);
}
