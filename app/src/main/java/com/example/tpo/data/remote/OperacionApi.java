package com.example.tpo.data.remote;

import com.example.tpo.data.remote.dto.CalificarRequest;
import com.example.tpo.data.remote.dto.OperacionResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Endpoints del historial de operaciones y de calificaciones (Punto 9) según
 * {@code docs/contrato-api-perfil-historial.md}. La usa {@code OperacionRepositoryApi}.
 */
public interface OperacionApi {

    /**
     * Operaciones concretadas del usuario del token. Los parámetros en
     * {@code null} Retrofit no los manda: {@code null} = sin ese filtro.
     *
     * @param tipo  "COMPRA", "VENTA" o null (las dos).
     * @param desde epoch ms inclusivo, o null.
     * @param hasta epoch ms inclusivo, o null.
     */
    @GET("operaciones")
    Call<List<OperacionResponse>> obtenerHistorial(@Query("tipo") String tipo,
                                                   @Query("desde") Long desde,
                                                   @Query("hasta") Long hasta);

    /** Responde la operación actualizada (ya con {@code mi_calificacion}). */
    @POST("operaciones/{id}/calificacion")
    Call<OperacionResponse> calificar(@Path("id") String operacionId, @Body CalificarRequest datos);
}
