package com.example.tpo.data;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.tpo.di.RetrofitEntryPoint;
import com.example.tpo.data.remote.ErrorApi;
import com.example.tpo.data.remote.FavoritoApi;
import com.example.tpo.data.remote.dto.FavoritoNuevoRequest;
import com.example.tpo.data.remote.dto.FavoritoResponse;
import com.example.tpo.model.Publicacion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class FavoritoRepositoryApi implements FavoritoRepository {

    private static FavoritoRepositoryApi instancia;

    private final FavoritoApi api;

    private final Map<String, Publicacion> favoritos = new LinkedHashMap<>();
    /** Precio que tenía la publicación cuando se marcó como favorita, para saber si subió o bajó. */
    private final Map<String, Double> precioAlGuardar = new HashMap<>();
    private final Set<String> conNovedad = new HashSet<>();

    private volatile boolean vistoPendiente = false;

    private FavoritoRepositoryApi(FavoritoApi api) {
        this.api = api;
    }

    public static synchronized FavoritoRepositoryApi getInstancia(Context context) {
        if (instancia == null) {
            Retrofit retrofit = RetrofitEntryPoint.obtenerRetrofit(context);
            instancia = new FavoritoRepositoryApi(retrofit.create(FavoritoApi.class));
        }
        return instancia;
    }

    static synchronized FavoritoRepositoryApi instanciaActual() {
        return instancia;
    }

    @Override
    public void precargar(RepositorioCallback<Void> callback) {
        listar(new RepositorioCallback<List<Publicacion>>() {
            @Override
            public void onExito(List<Publicacion> resultado) {
                callback.onExito(null);
            }

            @Override
            public void onError(String mensaje) {
                callback.onError(mensaje);
            }
        });
    }

    @Override
    public boolean esFavorito(String publicacionId) {
        return favoritos.containsKey(publicacionId);
    }

    @Override
    public void marcar(Publicacion publicacion, RepositorioCallback<Void> callback) {
        api.marcar(new FavoritoNuevoRequest(publicacion.getId())).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError(ErrorApi.mensaje(response, "No pudimos guardar el favorito"));
                    return;
                }
                favoritos.put(publicacion.getId(), publicacion);
                precioAlGuardar.put(publicacion.getId(), publicacion.getPrecio());
                conNovedad.remove(publicacion.getId());
                callback.onExito(null);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        });
    }

    @Override
    public void desmarcar(String publicacionId, RepositorioCallback<Void> callback) {
        api.desmarcar(publicacionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError(ErrorApi.mensaje(response, "No pudimos quitar el favorito"));
                    return;
                }
                favoritos.remove(publicacionId);
                precioAlGuardar.remove(publicacionId);
                conNovedad.remove(publicacionId);
                callback.onExito(null);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        });
    }

    @Override
    public void listar(RepositorioCallback<List<Publicacion>> callback) {
        api.listar().enqueue(new Callback<List<FavoritoResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<FavoritoResponse>> call,
                                   @NonNull Response<List<FavoritoResponse>> response) {
                List<FavoritoResponse> cuerpo = response.body();
                if (!response.isSuccessful() || cuerpo == null) {
                    callback.onError(ErrorApi.mensaje(response, "No pudimos cargar tus favoritos"));
                    return;
                }
                List<Publicacion> resultado;
                try {
                    resultado = actualizarCache(cuerpo);
                } catch (RuntimeException excepcion) {
                    callback.onError("No pudimos cargar tus favoritos");
                    return;
                }
                callback.onExito(resultado);
            }

            @Override
            public void onFailure(@NonNull Call<List<FavoritoResponse>> call, @NonNull Throwable throwable) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        });
    }

    private List<Publicacion> actualizarCache(List<FavoritoResponse> json) {
        favoritos.clear();
        precioAlGuardar.clear();
        conNovedad.clear();
        List<Publicacion> resultado = new ArrayList<>();
        for (FavoritoResponse item : json) {
            Publicacion publicacion = item.aModelo();
            if (publicacion == null) {
                // Valor de enum que la app no conoce: se descarta esta sola,
                // no hace falta tirar todo el listado de favoritos abajo.
                continue;
            }
            favoritos.put(publicacion.getId(), publicacion);
            precioAlGuardar.put(publicacion.getId(), item.precioAlGuardar);
            boolean cambioDePrecio = item.precio != item.precioAlGuardar;
            // Si el usuario ya vio este mismo cambio por el lado de una búsqueda
            // guardada, no corresponde volver a mostrarlo acá como novedad nueva.
            boolean yaReconocido = NovedadesDePrecio.estaReconocido(publicacion.getId(), item.precio);
            if (!vistoPendiente && cambioDePrecio && !yaReconocido) {
                conNovedad.add(publicacion.getId());
            }
            resultado.add(publicacion);
        }
        return resultado;
    }

    @Override
    public boolean tieneNovedad(String publicacionId) {
        return conNovedad.contains(publicacionId);
    }

    @Override
    public boolean hayAlgunaNovedad() {
        return !conNovedad.isEmpty();
    }

    void quitarNovedad(String publicacionId) {
        conNovedad.remove(publicacionId);
    }

    @Override
    public boolean subioDePrecio(String publicacionId) {
        Publicacion publicacion = favoritos.get(publicacionId);
        Double anterior = precioAlGuardar.get(publicacionId);
        return publicacion != null && anterior != null && publicacion.getPrecio() > anterior;
    }

    @Override
    public void marcarTodoVisto() {
        // Se anota acá antes de limpiar: si alguna de estas publicaciones
        // también matchea una búsqueda guardada, esa búsqueda tiene que dar
        // por vista la misma novedad de precio (ver NovedadesDePrecio).
        BusquedaGuardadaRepositoryApi busquedas = BusquedaGuardadaRepositoryApi.instanciaActual();
        for (String id : conNovedad) {
            Publicacion publicacion = favoritos.get(id);
            if (publicacion != null) {
                NovedadesDePrecio.reconocer(id, publicacion.getPrecio());
                if (busquedas != null) {
                    busquedas.quitarNovedadDePrecio(id);
                }
            }
        }
        conNovedad.clear();
        vistoPendiente = true;
        api.marcarTodoVisto().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                vistoPendiente = false;
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
                vistoPendiente = false;
            }
        });
    }
}
