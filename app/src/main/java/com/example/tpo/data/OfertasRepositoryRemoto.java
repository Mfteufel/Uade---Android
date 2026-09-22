package com.example.tpo.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tpo.data.remote.ErrorApi;
import com.example.tpo.data.remote.OfertasApi;
import com.example.tpo.data.remote.dto.CambioEstadoOfertaRequest;
import com.example.tpo.data.remote.dto.CambioPrecioOfertaRequest;
import com.example.tpo.data.remote.dto.OfertaNuevaRequest;
import com.example.tpo.data.remote.dto.OfertaResponse;
import com.example.tpo.model.EstadoOferta;
import com.example.tpo.model.OfertaNegociacion;
import com.example.tpo.model.TurnoOferta;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Implementación de {@link OfertasRepository} con Retrofit, contra el backend
 * real de ofertas (versión 0.4.0). Única implementación: el backend ya existe,
 * no hay capa mock.
 */
public class OfertasRepositoryRemoto implements OfertasRepository {

    /** Mensaje para {@code onFailure} (sin red, timeout): no hubo respuesta del servidor. */
    private static final String SIN_CONEXION = "Necesitás conexión para continuar";

    private final OfertasApi api;

    public OfertasRepositoryRemoto(OfertasApi api) {
        this.api = api;
    }

    @Override
    public void crear(String publicacionId, double precio, @Nullable String mensaje,
                      RepositorioCallback<OfertaNegociacion> callback) {
        api.crear(new OfertaNuevaRequest(publicacionId, precio, mensaje))
                .enqueue(callbackDeUna(callback, "No pudimos enviar la oferta"));
    }

    @Override
    public void enviadas(RepositorioCallback<List<OfertaNegociacion>> callback) {
        api.enviadas().enqueue(callbackDeVarias(callback, "No pudimos cargar tus ofertas enviadas"));
    }

    @Override
    public void recibidas(RepositorioCallback<List<OfertaNegociacion>> callback) {
        api.recibidas().enqueue(callbackDeVarias(callback, "No pudimos cargar tus ofertas recibidas"));
    }

    @Override
    public void obtener(String ofertaId, RepositorioCallback<OfertaNegociacion> callback) {
        api.obtener(ofertaId).enqueue(callbackDeUna(callback, "No pudimos cargar esta oferta"));
    }

    @Override
    public void aceptar(String ofertaId, RepositorioCallback<OfertaNegociacion> callback) {
        api.cambiarEstado(ofertaId, new CambioEstadoOfertaRequest("ACEPTADA"))
                .enqueue(callbackDeUna(callback, "No pudimos aceptar la oferta"));
    }

    @Override
    public void rechazar(String ofertaId, RepositorioCallback<OfertaNegociacion> callback) {
        api.cambiarEstado(ofertaId, new CambioEstadoOfertaRequest("RECHAZADA"))
                .enqueue(callbackDeUna(callback, "No pudimos rechazar la oferta"));
    }

    @Override
    public void contraofertar(String ofertaId, double precio, RepositorioCallback<OfertaNegociacion> callback) {
        api.cambiarPrecio(ofertaId, new CambioPrecioOfertaRequest(precio))
                .enqueue(callbackDeUna(callback, "No pudimos enviar la contraoferta"));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Callback<OfertaResponse> callbackDeUna(RepositorioCallback<OfertaNegociacion> callback,
                                                    String mensajeError) {
        return new Callback<OfertaResponse>() {
            @Override
            public void onResponse(@NonNull Call<OfertaResponse> call, @NonNull Response<OfertaResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(ErrorApi.mensaje(response, mensajeError));
                    return;
                }
                callback.onExito(haciaModelo(response.body()));
            }

            @Override
            public void onFailure(@NonNull Call<OfertaResponse> call, @NonNull Throwable throwable) {
                callback.onError(SIN_CONEXION);
            }
        };
    }

    private Callback<List<OfertaResponse>> callbackDeVarias(RepositorioCallback<List<OfertaNegociacion>> callback,
                                                             String mensajeError) {
        return new Callback<List<OfertaResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<OfertaResponse>> call,
                                   @NonNull Response<List<OfertaResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(ErrorApi.mensaje(response, mensajeError));
                    return;
                }
                List<OfertaNegociacion> resultado = new ArrayList<>();
                for (OfertaResponse respuesta : response.body()) {
                    resultado.add(haciaModelo(respuesta));
                }
                callback.onExito(resultado);
            }

            @Override
            public void onFailure(@NonNull Call<List<OfertaResponse>> call, @NonNull Throwable throwable) {
                callback.onError(SIN_CONEXION);
            }
        };
    }

    private static OfertaNegociacion haciaModelo(OfertaResponse respuesta) {
        return new OfertaNegociacion(
                respuesta.getId(), respuesta.getPublicacionId(), respuesta.getTituloPublicacion(),
                respuesta.getFotoPrincipalUrl(), respuesta.getCompradorId(), respuesta.getNombreComprador(),
                respuesta.getVendedorId(), respuesta.getNombreVendedor(), respuesta.getPrecio(),
                respuesta.getMensaje(), EstadoOferta.valueOf(respuesta.getEstado()),
                TurnoOferta.valueOf(respuesta.getTurno()), respuesta.getFechaCreacion(),
                respuesta.getVenceEn(), respuesta.getDireccionEntrega());
    }
}
