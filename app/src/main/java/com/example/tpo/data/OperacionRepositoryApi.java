package com.example.tpo.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tpo.data.remote.ErrorApi;
import com.example.tpo.data.remote.OperacionApi;
import com.example.tpo.data.remote.dto.CalificarRequest;
import com.example.tpo.data.remote.dto.OperacionResponse;
import com.example.tpo.model.FiltroOperaciones;
import com.example.tpo.model.Operacion;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Implementación de {@link OperacionRepository} contra la API REST, con Retrofit.
 * <p>
 * <b>Todavía no se inyecta</b>: se activa en {@code di/RepositoryModule} cuando el
 * backend esté levantado. Las reglas para calificar las valida el servidor; si
 * rechaza (403/409/422), el {@code detail} llega tal cual por {@code onError},
 * igual que los mensajes que hoy arma {@link BaseDeDatosMock}.
 */
public class OperacionRepositoryApi implements OperacionRepository {

    private final OperacionApi api;

    public OperacionRepositoryApi(OperacionApi api) {
        this.api = api;
    }

    @Override
    public void obtenerHistorial(FiltroOperaciones filtro,
                                 RepositorioCallback<List<Operacion>> callback) {
        String tipo = filtro.getTipo() == null ? null : filtro.getTipo().name();
        api.obtenerHistorial(tipo, filtro.getDesde(), filtro.getHasta())
                .enqueue(new Callback<List<OperacionResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<OperacionResponse>> llamada,
                                           @NonNull Response<List<OperacionResponse>> respuesta) {
                        List<OperacionResponse> cuerpo = respuesta.body();
                        if (!respuesta.isSuccessful() || cuerpo == null) {
                            callback.onError(ErrorApi.mensaje(respuesta, "No pudimos cargar tu historial"));
                            return;
                        }
                        List<Operacion> operaciones = new ArrayList<>();
                        try {
                            for (OperacionResponse json : cuerpo) {
                                operaciones.add(json.aModelo());
                            }
                        } catch (RuntimeException excepcion) {
                            // JSON distinto al contrato (campo faltante, enum desconocido).
                            callback.onError("No pudimos cargar tu historial");
                            return;
                        }
                        callback.onExito(operaciones);
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<OperacionResponse>> llamada,
                                          @NonNull Throwable error) {
                        callback.onError(ErrorApi.SIN_CONEXION);
                    }
                });
    }

    @Override
    public void calificar(String operacionId, int estrellas, @Nullable String comentario,
                          RepositorioCallback<Operacion> callback) {
        api.calificar(operacionId, new CalificarRequest(estrellas, comentario))
                .enqueue(new Callback<OperacionResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OperacionResponse> llamada,
                                           @NonNull Response<OperacionResponse> respuesta) {
                        OperacionResponse cuerpo = respuesta.body();
                        if (!respuesta.isSuccessful() || cuerpo == null) {
                            callback.onError(ErrorApi.mensaje(respuesta, "No pudimos enviar tu calificación"));
                            return;
                        }
                        Operacion actualizada;
                        try {
                            actualizada = cuerpo.aModelo();
                        } catch (RuntimeException excepcion) {
                            callback.onError("No pudimos enviar tu calificación");
                            return;
                        }
                        callback.onExito(actualizada);
                    }

                    @Override
                    public void onFailure(@NonNull Call<OperacionResponse> llamada,
                                          @NonNull Throwable error) {
                        callback.onError(ErrorApi.SIN_CONEXION);
                    }
                });
    }
}
