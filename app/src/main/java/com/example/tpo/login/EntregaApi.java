package com.example.tpo.login;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface EntregaApi {

    // datos de la venta para el panel; el enlace al QR solo le llega al vendedor
    @GET("operaciones/{id}/entrega")
    Call<EntregaResponse> verEntrega(@Path("id") String operacionId);

    // el comprador manda el texto que leyo del QR: el servidor decide si es el de esta venta
    @POST("operaciones/{id}/entrega")
    Call<EntregaResponse> confirmarEntrega(@Path("id") String operacionId,
                                           @Body CodigoEntregaRequest body);
}
