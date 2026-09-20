package com.example.tpo.data;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.tpo.model.FiltroOperaciones;
import com.example.tpo.model.Operacion;

import java.util.List;

/**
 * Implementación de {@link OperacionRepository} sin servidor, sobre
 * {@link BaseDeDatosMock}.
 * <p>
 * Las reglas para calificar no están acá sino en la base simulada, igual que en
 * el backend real van en el servidor: este mock solo agrega la demora de red, la
 * identidad del que pide (que la API sacaría del token) y el Main Thread.
 */
public class OperacionRepositoryMock implements OperacionRepository {

    /** Demora artificial de la respuesta, para que se vea el estado de carga. */
    private static final long DEMORA_SIMULADA_MS = 500;

    /**
     * Poner en true para probar la pantalla de error sin backend caído.
     * Debe quedar en false en lo que se entrega.
     */
    private static final boolean SIMULAR_ERROR = false;

    private final BaseDeDatosMock base = BaseDeDatosMock.getInstancia();

    /** Handler del Main Thread: garantiza que el callback llegue donde se puede tocar la UI. */
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    @Override
    public void obtenerHistorial(FiltroOperaciones filtro,
                                 RepositorioCallback<List<Operacion>> callback) {
        handlerPrincipal.postDelayed(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos cargar tu historial");
                return;
            }
            // Desde > hasta llegaría como 400 desde la API.
            if (filtro.getDesde() != null && filtro.getHasta() != null
                    && filtro.getDesde() > filtro.getHasta()) {
                callback.onError("La fecha de inicio no puede ser posterior a la de fin");
                return;
            }
            callback.onExito(base.historial(idLogueado(), filtro, System.currentTimeMillis()));
        }, DEMORA_SIMULADA_MS);
    }

    @Override
    public void calificar(String operacionId, int estrellas, @Nullable String comentario,
                          RepositorioCallback<Operacion> callback) {
        handlerPrincipal.postDelayed(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos enviar tu calificación");
                return;
            }
            long ahora = System.currentTimeMillis();
            String error = base.validarCalificacion(operacionId, idLogueado(), estrellas,
                    comentario, ahora);
            if (error != null) {
                // Equivale a los 403/409/422 de la API, con su "detail".
                callback.onError(error);
                return;
            }
            callback.onExito(base.calificar(operacionId, idLogueado(), estrellas, comentario, ahora));
        }, DEMORA_SIMULADA_MS);
    }

    /** Quién está pidiendo. Contra la API real lo resuelve el servidor a partir del JWT. */
    private static String idLogueado() {
        return SesionUsuario.getInstancia().getUsuarioId();
    }
}
