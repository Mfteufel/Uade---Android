package com.example.tpo.data.remote;

import com.example.tpo.data.remote.dto.CambiarEstadoPublicacionRequest;
import com.example.tpo.data.remote.dto.PaginaPublicacionesResponse;
import com.example.tpo.data.remote.dto.PublicacionDetalleResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PublicacionApi {

    @GET("publicaciones")
    Call<PaginaPublicacionesResponse> buscar(
            @Query("q") String texto,
            @Query("categoria") String categoria,
            @Query("estado") List<String> estados,
            @Query("zona") List<String> zonas,
            @Query("precio_min") Double precioMinimo,
            @Query("precio_max") Double precioMaximo,
            @Query("vendedorId") String vendedorId,
            @Query("orden") String orden,
            @Query("pagina") int pagina);

    @GET("publicaciones/{id}")
    Call<PublicacionDetalleResponse> obtenerDetalle(@Path("id") String id);

    @PATCH("publicaciones/{id}/estado")
    Call<Void> cambiarEstado(@Path("id") String id, @Body CambiarEstadoPublicacionRequest request);
}
