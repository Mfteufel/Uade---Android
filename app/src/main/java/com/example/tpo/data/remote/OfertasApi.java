package com.example.tpo.data.remote;

import com.example.tpo.data.remote.dto.CambioEstadoOfertaRequest;
import com.example.tpo.data.remote.dto.CambioPrecioOfertaRequest;
import com.example.tpo.data.remote.dto.OfertaNuevaRequest;
import com.example.tpo.data.remote.dto.OfertaResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Contrato con la API real del TPO para el Punto 7 (Ofertas y Negociación),
 * versión 0.4.0 del backend. {@code PATCH} para aceptar/rechazar/contraofertar
 * porque la URL nombra la oferta y el verbo HTTP dice qué se le hace — no hay
 * verbos en la URL (ver anexo de coloquio del equipo).
 */
public interface OfertasApi {

    @POST("ofertas")
    Call<OfertaResponse> crear(@Body OfertaNuevaRequest request);

    @GET("ofertas/enviadas")
    Call<List<OfertaResponse>> enviadas();

    @GET("ofertas/recibidas")
    Call<List<OfertaResponse>> recibidas();

    @GET("ofertas/{id}")
    Call<OfertaResponse> obtener(@Path("id") String id);

    @PATCH("ofertas/{id}/estado")
    Call<OfertaResponse> cambiarEstado(@Path("id") String id, @Body CambioEstadoOfertaRequest request);

    @PATCH("ofertas/{id}/precio")
    Call<OfertaResponse> cambiarPrecio(@Path("id") String id, @Body CambioPrecioOfertaRequest request);
}
