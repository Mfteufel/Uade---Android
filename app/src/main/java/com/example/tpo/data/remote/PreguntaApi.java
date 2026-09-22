package com.example.tpo.data.remote;

import com.example.tpo.data.remote.dto.PreguntaNuevaRequest;
import com.example.tpo.data.remote.dto.PreguntaResponse;
import com.example.tpo.data.remote.dto.RespuestaNuevaRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Endpoints de preguntas de una publicación (Punto 4). Públicas: cualquiera las lista. */
public interface PreguntaApi {

    @GET("publicaciones/{id}/preguntas")
    Call<List<PreguntaResponse>> listar(@Path("id") String publicacionId);

    @POST("publicaciones/{id}/preguntas")
    Call<PreguntaResponse> preguntar(@Path("id") String publicacionId, @Body PreguntaNuevaRequest request);

    /** Solo el dueño de la publicación puede contestar (el backend lo valida igual). */
    @POST("publicaciones/{id}/preguntas/{preguntaId}/respuesta")
    Call<PreguntaResponse> responder(@Path("id") String publicacionId,
                                     @Path("preguntaId") String preguntaId,
                                     @Body RespuestaNuevaRequest request);
}
