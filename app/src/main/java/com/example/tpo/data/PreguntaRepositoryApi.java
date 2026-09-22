package com.example.tpo.data;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.tpo.data.remote.ErrorApi;
import com.example.tpo.data.remote.PreguntaApi;
import com.example.tpo.data.remote.dto.PreguntaNuevaRequest;
import com.example.tpo.data.remote.dto.PreguntaResponse;
import com.example.tpo.di.RetrofitEntryPoint;
import com.example.tpo.model.Pregunta;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Implementación de {@link PreguntaRepository} con Retrofit, contra el backend
 * real (único, no hay capa mock). Mismo patrón que {@link FavoritoRepositoryApi}:
 * {@code getInstancia(Context)} resuelve el {@link Retrofit} compartido vía
 * {@link RetrofitEntryPoint} en vez de recibirlo por Hilt {@code @Inject}
 * directo, porque quien la usa ({@code DetalleFragment}, mismo criterio que
 * {@code PublicacionRepositoryApi}) la resuelve en {@code onAttach(Context)}.
 */
public class PreguntaRepositoryApi implements PreguntaRepository {

    private static PreguntaRepositoryApi instancia;

    private final PreguntaApi api;

    private PreguntaRepositoryApi(PreguntaApi api) {
        this.api = api;
    }

    public static synchronized PreguntaRepositoryApi getInstancia(Context context) {
        if (instancia == null) {
            Retrofit retrofit = RetrofitEntryPoint.obtenerRetrofit(context);
            instancia = new PreguntaRepositoryApi(retrofit.create(PreguntaApi.class));
        }
        return instancia;
    }

    @Override
    public void deLaPublicacion(String publicacionId, RepositorioCallback<List<Pregunta>> callback) {
        api.listar(publicacionId).enqueue(new Callback<List<PreguntaResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<PreguntaResponse>> call,
                                   @NonNull Response<List<PreguntaResponse>> response) {
                List<PreguntaResponse> cuerpo = response.body();
                if (!response.isSuccessful() || cuerpo == null) {
                    callback.onError(ErrorApi.mensaje(response, "No pudimos cargar las preguntas"));
                    return;
                }
                List<Pregunta> resultado = new ArrayList<>();
                for (PreguntaResponse respuesta : cuerpo) {
                    resultado.add(respuesta.aModelo());
                }
                callback.onExito(resultado);
            }

            @Override
            public void onFailure(@NonNull Call<List<PreguntaResponse>> call, @NonNull Throwable throwable) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        });
    }

    @Override
    public void crear(String publicacionId, String texto, RepositorioCallback<Pregunta> callback) {
        api.preguntar(publicacionId, new PreguntaNuevaRequest(texto)).enqueue(new Callback<PreguntaResponse>() {
            @Override
            public void onResponse(@NonNull Call<PreguntaResponse> call,
                                   @NonNull Response<PreguntaResponse> response) {
                PreguntaResponse cuerpo = response.body();
                if (!response.isSuccessful() || cuerpo == null) {
                    callback.onError(ErrorApi.mensaje(response, "No pudimos enviar la pregunta"));
                    return;
                }
                callback.onExito(cuerpo.aModelo());
            }

            @Override
            public void onFailure(@NonNull Call<PreguntaResponse> call, @NonNull Throwable throwable) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        });
    }
}
