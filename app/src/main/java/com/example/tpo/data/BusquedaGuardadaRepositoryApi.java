package com.example.tpo.data;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.tpo.di.RetrofitEntryPoint;
import com.example.tpo.data.remote.BusquedaApi;
import com.example.tpo.data.remote.ErrorApi;
import com.example.tpo.data.remote.FiltroMapper;
import com.example.tpo.data.remote.PublicacionApi;
import com.example.tpo.data.remote.dto.BusquedaNuevaRequest;
import com.example.tpo.data.remote.dto.BusquedaResponse;
import com.example.tpo.data.remote.dto.FiltroGuardadoDto;
import com.example.tpo.data.remote.dto.PaginaPublicacionesResponse;
import com.example.tpo.data.remote.dto.PublicacionResumenResponse;
import com.example.tpo.model.BusquedaGuardada;
import com.example.tpo.model.FiltroPublicaciones;
import com.example.tpo.model.OrdenPublicaciones;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class BusquedaGuardadaRepositoryApi implements BusquedaGuardadaRepository {

    private static BusquedaGuardadaRepositoryApi instancia;

    private final BusquedaApi api;
    private final PublicacionApi publicacionApi;

    private final Map<String, BusquedaResponse> cache = new LinkedHashMap<>();
    /** id de búsqueda -> (id de publicación -> precio) la última vez que el usuario la aplicó. */
    private final Map<String, Map<String, Double>> snapshotVisto = new HashMap<>();
    /** id de búsqueda -> (id de publicación -> precio) de la resolución más reciente, para poder fijar el snapshot en marcarVisto() sin otro pedido de red. */
    private final Map<String, Map<String, Double>> preciosUltimaConsulta = new HashMap<>();
    private final Map<String, Set<String>> nuevasPorBusqueda = new HashMap<>();
    private final Map<String, Map<String, Boolean>> cambiosDePrecioPorBusqueda = new HashMap<>();

    private BusquedaGuardadaRepositoryApi(BusquedaApi api, PublicacionApi publicacionApi) {
        this.api = api;
        this.publicacionApi = publicacionApi;
    }

    public static synchronized BusquedaGuardadaRepositoryApi getInstancia(Context context) {
        if (instancia == null) {
            Retrofit retrofit = RetrofitEntryPoint.obtenerRetrofit(context);
            instancia = new BusquedaGuardadaRepositoryApi(
                    retrofit.create(BusquedaApi.class),
                    retrofit.create(PublicacionApi.class));
        }
        return instancia;
    }

    static synchronized BusquedaGuardadaRepositoryApi instanciaActual() {
        return instancia;
    }

    @Override
    public void precargar(RepositorioCallback<Void> callback) {
        listar(new RepositorioCallback<List<BusquedaGuardada>>() {
            @Override
            public void onExito(List<BusquedaGuardada> resultado) {
                callback.onExito(null);
            }

            @Override
            public void onError(String mensaje) {
                callback.onError(mensaje);
            }
        });
    }

    @Override
    public void guardar(String nombre, FiltroPublicaciones filtro, RepositorioCallback<Void> callback) {
        for (BusquedaResponse existente : cache.values()) {
            if (FiltroMapper.desdeDto(existente.filtro).equals(filtro)) {
                callback.onError("Ya tenés guardada una búsqueda con estos mismos filtros");
                return;
            }
        }

        FiltroGuardadoDto dto = FiltroMapper.aDto(filtro);
        api.guardar(new BusquedaNuevaRequest(nombre, dto)).enqueue(new Callback<BusquedaResponse>() {
            @Override
            public void onResponse(@NonNull Call<BusquedaResponse> call, @NonNull Response<BusquedaResponse> response) {
                BusquedaResponse cuerpo = response.body();
                if (!response.isSuccessful() || cuerpo == null) {
                    callback.onError(ErrorApi.mensaje(response, "No pudimos guardar la búsqueda"));
                    return;
                }
                cache.put(cuerpo.id, cuerpo);
                callback.onExito(null);
            }

            @Override
            public void onFailure(@NonNull Call<BusquedaResponse> call, @NonNull Throwable throwable) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        });
    }

    @Override
    public void listar(RepositorioCallback<List<BusquedaGuardada>> callback) {
        api.listar().enqueue(new Callback<List<BusquedaResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<BusquedaResponse>> call,
                                   @NonNull Response<List<BusquedaResponse>> response) {
                List<BusquedaResponse> cuerpo = response.body();
                if (!response.isSuccessful() || cuerpo == null) {
                    callback.onError(ErrorApi.mensaje(response, "No pudimos cargar tus búsquedas guardadas"));
                    return;
                }
                List<BusquedaGuardada> resultado;
                try {
                    resultado = actualizarCache(cuerpo);
                } catch (RuntimeException excepcion) {
                    callback.onError("No pudimos cargar tus búsquedas guardadas");
                    return;
                }
                resolverNovedades(cuerpo, () -> callback.onExito(resultado));
            }

            @Override
            public void onFailure(@NonNull Call<List<BusquedaResponse>> call, @NonNull Throwable throwable) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        });
    }

    private List<BusquedaGuardada> actualizarCache(List<BusquedaResponse> json) {
        cache.clear();
        List<BusquedaGuardada> resultado = new ArrayList<>();
        for (BusquedaResponse item : json) {
            cache.put(item.id, item);
            resultado.add(aModelo(item));
        }
        return resultado;
    }

    /** Resuelve, para cada búsqueda guardada, cuáles matches son nuevos y cuáles cambiaron de precio (ver el javadoc de la clase). */
    private void resolverNovedades(List<BusquedaResponse> busquedas, Runnable alTerminar) {
        nuevasPorBusqueda.clear();
        cambiosDePrecioPorBusqueda.clear();
        if (busquedas.isEmpty()) {
            alTerminar.run();
            return;
        }
        AtomicInteger pendientes = new AtomicInteger(busquedas.size());
        for (BusquedaResponse busqueda : busquedas) {
            resolverUnaBusqueda(busqueda, () -> {
                if (pendientes.decrementAndGet() == 0) {
                    alTerminar.run();
                }
            });
        }
    }

    /** Si esta búsqueda puntual falla, queda sin destacado (no corta la carga de las demás ni la de la lista en sí). */
    private void resolverUnaBusqueda(BusquedaResponse busqueda, Runnable alTerminar) {
        FiltroPublicaciones filtro = FiltroMapper.desdeDto(busqueda.filtro);
        publicacionApi.buscar(FiltroMapper.textoDe(filtro), FiltroMapper.categoriaDe(filtro),
                        FiltroMapper.estadosDe(filtro), FiltroMapper.zonasDe(filtro),
                        filtro.getPrecioMinimo(), filtro.getPrecioMaximo(),
                        null, OrdenPublicaciones.RECIENTES.name(), 0)
                .enqueue(new Callback<PaginaPublicacionesResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<PaginaPublicacionesResponse> call,
                                           @NonNull Response<PaginaPublicacionesResponse> response) {
                        PaginaPublicacionesResponse cuerpo = response.body();
                        if (!response.isSuccessful() || cuerpo == null) {
                            alTerminar.run();
                            return;
                        }
                        compararConSnapshot(busqueda, cuerpo.publicaciones);
                        alTerminar.run();
                    }

                    @Override
                    public void onFailure(@NonNull Call<PaginaPublicacionesResponse> call,
                                          @NonNull Throwable throwable) {
                        alTerminar.run();
                    }
                });
    }

    private void compararConSnapshot(BusquedaResponse busqueda, List<PublicacionResumenResponse> publicaciones) {
        Map<String, Double> actuales = new HashMap<>();
        for (PublicacionResumenResponse item : publicaciones) {
            actuales.put(item.id, item.precio);
        }
        preciosUltimaConsulta.put(busqueda.id, actuales);

        Map<String, Double> snapshot = snapshotVisto.get(busqueda.id);
        if (snapshot == null) {
            snapshotVisto.put(busqueda.id, new HashMap<>(actuales));
            return;
        }

        Set<String> nuevas = new HashSet<>();
        Map<String, Boolean> cambios = new HashMap<>();
        for (Map.Entry<String, Double> entry : actuales.entrySet()) {
            String id = entry.getKey();
            double precioActual = entry.getValue();
            Double precioVisto = snapshot.get(id);
            if (precioVisto == null) {
                nuevas.add(id);
            } else if (!precioVisto.equals(precioActual)) {
                if (NovedadesDePrecio.estaReconocido(id, precioActual)) {
                    snapshot.put(id, precioActual);
                } else {
                    cambios.put(id, precioActual > precioVisto);
                }
            }
        }
        nuevasPorBusqueda.put(busqueda.id, nuevas);
        cambiosDePrecioPorBusqueda.put(busqueda.id, cambios);
    }

    private static BusquedaGuardada aModelo(BusquedaResponse json) {
        FiltroPublicaciones filtro = FiltroMapper.desdeDto(json.filtro);
        return new BusquedaGuardada(json.id, json.nombre, filtro, json.fechaGuardado);
    }

    @Override
    public void eliminar(String id, RepositorioCallback<Void> callback) {
        api.eliminar(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError(ErrorApi.mensaje(response, "No pudimos eliminar la búsqueda"));
                    return;
                }
                cache.remove(id);
                nuevasPorBusqueda.remove(id);
                cambiosDePrecioPorBusqueda.remove(id);
                snapshotVisto.remove(id);
                preciosUltimaConsulta.remove(id);
                callback.onExito(null);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        });
    }

    @Override
    public boolean tieneNovedad(String id) {
        return !publicacionesNuevasDe(id).isEmpty() || !publicacionesConCambioDePrecioDe(id).isEmpty();
    }

    @Override
    public boolean hayAlgunaNovedad() {
        for (Set<String> nuevas : nuevasPorBusqueda.values()) {
            if (!nuevas.isEmpty()) {
                return true;
            }
        }
        for (Map<String, Boolean> cambios : cambiosDePrecioPorBusqueda.values()) {
            if (!cambios.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void marcarVisto(String id) {
        Map<String, Double> ultimaConsulta = preciosUltimaConsulta.get(id);
        if (ultimaConsulta != null) {
            snapshotVisto.put(id, new HashMap<>(ultimaConsulta));
        }
        // Si alguna de las publicaciones que cambiaron de precio acá también es
        // favorita, Favoritos tiene que dar por vista la misma novedad (ver
        // NovedadesDePrecio)
        Map<String, Boolean> cambios = cambiosDePrecioPorBusqueda.get(id);
        if (cambios != null && ultimaConsulta != null) {
            FavoritoRepositoryApi favoritos = FavoritoRepositoryApi.instanciaActual();
            for (String publicacionId : cambios.keySet()) {
                Double precioActual = ultimaConsulta.get(publicacionId);
                if (precioActual != null) {
                    NovedadesDePrecio.reconocer(publicacionId, precioActual);
                    if (favoritos != null) {
                        favoritos.quitarNovedad(publicacionId);
                    }
                }
            }
        }
        nuevasPorBusqueda.remove(id);
        cambiosDePrecioPorBusqueda.remove(id);
    }

    void quitarNovedadDePrecio(String publicacionId) {
        for (Map<String, Boolean> cambios : cambiosDePrecioPorBusqueda.values()) {
            cambios.remove(publicacionId);
        }
    }

    @Override
    public Set<String> publicacionesNuevasDe(String id) {
        Set<String> nuevas = nuevasPorBusqueda.get(id);
        return nuevas != null ? nuevas : Collections.emptySet();
    }

    @Override
    public Map<String, Boolean> publicacionesConCambioDePrecioDe(String id) {
        Map<String, Boolean> cambios = cambiosDePrecioPorBusqueda.get(id);
        return cambios != null ? cambios : Collections.emptyMap();
    }
}
