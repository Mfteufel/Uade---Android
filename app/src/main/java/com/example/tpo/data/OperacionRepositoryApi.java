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
 * Se activa en {@code di/RepositoryModule}. Las reglas para confirmar la entrega
 * y para calificar las valida el servidor; si rechaza (400/403/404/409), el
 * {@code detail} llega tal cual por {@code onError}.
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
                .enqueue(listaDeOperaciones(callback, "No pudimos cargar tu historial"));
    }

    @Override
    public void obtenerPendientesDeEntrega(RepositorioCallback<List<Operacion>> callback) {
        api.obtenerPendientesDeEntrega()
                .enqueue(listaDeOperaciones(callback, "No pudimos cargar las entregas pendientes"));
    }

    @Override
    public void confirmarEntrega(String operacionId, RepositorioCallback<Operacion> callback) {
        api.confirmarEntrega(operacionId)
                .enqueue(operacionActualizada(callback, "No pudimos confirmar la entrega"));
    }

    @Override
    public void calificar(String operacionId, int estrellas, @Nullable String comentario,
                          RepositorioCallback<Operacion> callback) {
        api.calificar(operacionId, new CalificarRequest(estrellas, comentario))
                .enqueue(operacionActualizada(callback, "No pudimos enviar tu calificación"));
    }

    /** El historial y las pendientes llegan igual: una lista de operaciones. */
    private static Callback<List<OperacionResponse>> listaDeOperaciones(
            RepositorioCallback<List<Operacion>> callback, String errorPorDefecto) {
        return new Callback<List<OperacionResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<OperacionResponse>> llamada,
                                   @NonNull Response<List<OperacionResponse>> respuesta) {
                List<OperacionResponse> cuerpo = respuesta.body();
                if (!respuesta.isSuccessful() || cuerpo == null) {
                    callback.onError(ErrorApi.mensaje(respuesta, errorPorDefecto));
                    return;
                }
                List<Operacion> operaciones = new ArrayList<>();
                try {
                    for (OperacionResponse json : cuerpo) {
                        operaciones.add(json.aModelo());
                    }
                } catch (RuntimeException excepcion) {
                    // JSON distinto al contrato (campo faltante, enum desconocido).
                    callback.onError(errorPorDefecto);
                    return;
                }
                callback.onExito(operaciones);
            }

            @Override
            public void onFailure(@NonNull Call<List<OperacionResponse>> llamada,
                                  @NonNull Throwable error) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        };
    }

    /**
     * Confirmar la entrega y calificar responden lo mismo: la operación ya
     * actualizada. 2xx con cuerpo es éxito; cualquier otro código trae el
     * {@code detail} del servidor; {@code onFailure} es falta de conexión.
     */
    private static Callback<OperacionResponse> operacionActualizada(
            RepositorioCallback<Operacion> callback, String errorPorDefecto) {
        return new Callback<OperacionResponse>() {
            @Override
            public void onResponse(@NonNull Call<OperacionResponse> llamada,
                                   @NonNull Response<OperacionResponse> respuesta) {
                OperacionResponse cuerpo = respuesta.body();
                if (!respuesta.isSuccessful() || cuerpo == null) {
                    callback.onError(ErrorApi.mensaje(respuesta, errorPorDefecto));
                    return;
                }
                Operacion actualizada;
                try {
                    actualizada = cuerpo.aModelo();
                } catch (RuntimeException excepcion) {
                    // JSON distinto al contrato (campo faltante, enum desconocido).
                    callback.onError(errorPorDefecto);
                    return;
                }
                callback.onExito(actualizada);
            }

            @Override
            public void onFailure(@NonNull Call<OperacionResponse> llamada,
                                  @NonNull Throwable error) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        };
    }
}
