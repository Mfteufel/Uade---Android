package com.example.tpo.data.remote;

import com.example.tpo.data.remote.dto.PreguntaNuevaRequest;
import com.example.tpo.data.remote.dto.PreguntaResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Endpoints de preguntas de una publicación (Punto 4). */
public interface PreguntaApi {

    @GET("publicaciones/{id}/preguntas")
    Call<List<PreguntaResponse>> listar(@Path("id") String publicacionId);

    @POST("publicaciones/{id}/preguntas")
    Call<PreguntaResponse> preguntar(@Path("id") String publicacionId, @Body PreguntaNuevaRequest request);
}
