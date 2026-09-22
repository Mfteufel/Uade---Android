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
 * Endpoints del historial de operaciones y de calificaciones (Punto 9). Del lado
 * del servidor una operación es una oferta aceptada (Punto 7); su id es el de la
 * oferta. La usa {@code OperacionRepositoryApi}.
 */
public interface OperacionApi {

    /**
     * Historial: operaciones concretadas (entrega confirmada) del usuario del
     * token, la entrega más reciente primero. Los parámetros en {@code null}
     * Retrofit no los manda: {@code null} = sin ese filtro.
     *
     * @param tipo  "COMPRA", "VENTA" o null (las dos).
     * @param desde epoch ms inclusivo, contra la fecha de entrega, o null.
     * @param hasta epoch ms inclusivo, contra la fecha de entrega, o null.
     */
    @GET("operaciones")
    Call<List<OperacionResponse>> obtenerHistorial(@Query("tipo") String tipo,
                                                   @Query("desde") Long desde,
                                                   @Query("hasta") Long hasta);

    /** Ventas aceptadas del usuario que todavía esperan la entrega. No llevan filtros. */
    @GET("operaciones/pendientes")
    Call<List<OperacionResponse>> obtenerPendientesDeEntrega();

    /** Solo el comprador. Responde la operación ya {@code ENTREGADA}. */
    @POST("operaciones/{id}/entrega")
    Call<OperacionResponse> confirmarEntrega(@Path("id") String operacionId);

    /** Responde la operación actualizada (ya con {@code miCalificacion}). */
    @POST("operaciones/{id}/calificacion")
    Call<OperacionResponse> calificar(@Path("id") String operacionId, @Body CalificarRequest datos);
}
