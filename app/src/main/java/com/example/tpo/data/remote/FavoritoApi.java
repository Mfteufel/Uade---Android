package com.example.tpo.data.remote;

import com.example.tpo.data.remote.dto.FavoritoNuevoRequest;
import com.example.tpo.data.remote.dto.FavoritoResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Endpoints de favoritos (Punto 10). */
public interface FavoritoApi {

    @GET("favoritos")
    Call<List<FavoritoResponse>> listar();

    @POST("favoritos")
    Call<Void> marcar(@Body FavoritoNuevoRequest request);

    @POST("favoritos/vistos")
    Call<Void> marcarTodoVisto();

    @DELETE("favoritos/{publicacionId}")
    Call<Void> desmarcar(@Path("publicacionId") String publicacionId);
}
