package com.example.tpo.data.remote;

import com.example.tpo.data.remote.dto.BusquedaNuevaRequest;
import com.example.tpo.data.remote.dto.BusquedaResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Endpoints de búsquedas guardadas (Punto 10).
 */
public interface BusquedaApi {

    @GET("busquedas")
    Call<List<BusquedaResponse>> listar();

    @POST("busquedas")
    Call<BusquedaResponse> guardar(@Body BusquedaNuevaRequest request);

    @DELETE("busquedas/{busquedaId}")
    Call<Void> eliminar(@Path("busquedaId") String busquedaId);
}
