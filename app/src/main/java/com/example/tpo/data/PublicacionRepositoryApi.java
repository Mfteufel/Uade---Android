package com.example.tpo.data;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.tpo.di.RetrofitEntryPoint;
import com.example.tpo.data.remote.ErrorApi;
import com.example.tpo.data.remote.FiltroMapper;
import com.example.tpo.data.remote.PublicacionApi;
import com.example.tpo.data.remote.UsuarioApi;
import com.example.tpo.data.remote.dto.CambiarEstadoPublicacionRequest;
import com.example.tpo.data.remote.dto.PaginaPublicacionesResponse;
import com.example.tpo.data.remote.dto.PublicacionDetalleResponse;
import com.example.tpo.data.remote.dto.PublicacionResumenResponse;
import com.example.tpo.data.remote.dto.UsuarioResponse;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.FiltroPublicaciones;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Vendedor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class PublicacionRepositoryApi implements PublicacionRepository {

    private static PublicacionRepositoryApi instancia;

    private final PublicacionApi api;
    private final UsuarioApi usuarioApi;

    private PublicacionRepositoryApi(PublicacionApi api, UsuarioApi usuarioApi) {
        this.api = api;
        this.usuarioApi = usuarioApi;
    }

    public static synchronized PublicacionRepositoryApi getInstancia(Context context) {
        if (instancia == null) {
            Retrofit retrofit = RetrofitEntryPoint.obtenerRetrofit(context);
            instancia = new PublicacionRepositoryApi(
                    retrofit.create(PublicacionApi.class),
                    retrofit.create(UsuarioApi.class));
        }
        return instancia;
    }

    @Override
    public void buscarPublicaciones(FiltroPublicaciones filtro, int pagina,
                                    RepositorioCallback<PaginaPublicaciones> callback) {
        buscar(filtro, null, pagina, callback);
    }

    private void buscar(FiltroPublicaciones filtro, String vendedorId, int pagina,
                        RepositorioCallback<PaginaPublicaciones> callback) {
        api.buscar(FiltroMapper.textoDe(filtro), FiltroMapper.categoriaDe(filtro),
                        FiltroMapper.estadosDe(filtro), FiltroMapper.zonasDe(filtro),
                        filtro.getPrecioMinimo(), filtro.getPrecioMaximo(),
                        vendedorId, filtro.getOrden().name(), pagina)
                .enqueue(new Callback<PaginaPublicacionesResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<PaginaPublicacionesResponse> call,
                                           @NonNull Response<PaginaPublicacionesResponse> response) {
                        PaginaPublicacionesResponse cuerpo = response.body();
                        if (!response.isSuccessful() || cuerpo == null) {
                            callback.onError(ErrorApi.mensaje(response, "No pudimos cargar las publicaciones"));
                            return;
                        }
                        try {
                            callback.onExito(aModelo(cuerpo));
                        } catch (RuntimeException excepcion) {
                            callback.onError("No pudimos cargar las publicaciones");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PaginaPublicacionesResponse> call,
                                          @NonNull Throwable throwable) {
                        callback.onError(ErrorApi.SIN_CONEXION);
                    }
                });
    }

    private static PaginaPublicaciones aModelo(PaginaPublicacionesResponse json) {
        List<Publicacion> publicaciones = new ArrayList<>();
        for (PublicacionResumenResponse item : json.publicaciones) {
            // Una publicación con un valor de enum que la app no conoce (por
            // ejemplo el backend agrega una categoría nueva) se descarta sola,
            // no tira la página entera abajo.
            Publicacion publicacion = item.aModelo();
            if (publicacion != null) {
                publicaciones.add(publicacion);
            }
        }
        return new PaginaPublicaciones(publicaciones, json.pagina, json.hayMas, json.totalResultados);
    }

    @Override
    public void obtenerPublicacion(String id, RepositorioCallback<Publicacion> callback) {
        api.obtenerDetalle(id).enqueue(new Callback<PublicacionDetalleResponse>() {
            @Override
            public void onResponse(@NonNull Call<PublicacionDetalleResponse> call,
                                   @NonNull Response<PublicacionDetalleResponse> response) {
                PublicacionDetalleResponse cuerpo = response.body();
                if (!response.isSuccessful() || cuerpo == null) {
                    callback.onError(ErrorApi.mensaje(response, "No pudimos cargar la publicación"));
                    return;
                }
                Publicacion sinVendedorReal = cuerpo.aModelo();
                if (sinVendedorReal == null) {
                    callback.onError("No pudimos cargar la publicación");
                    return;
                }
                completarVendedor(sinVendedorReal, cuerpo.vendedorId, callback);
            }

            @Override
            public void onFailure(@NonNull Call<PublicacionDetalleResponse> call, @NonNull Throwable throwable) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        });
    }

    private void completarVendedor(Publicacion publicacion, String vendedorId,
                                   RepositorioCallback<Publicacion> callback) {
        usuarioApi.obtenerPerfilPublico(vendedorId).enqueue(new Callback<UsuarioResponse>() {
            @Override
            public void onResponse(@NonNull Call<UsuarioResponse> call, @NonNull Response<UsuarioResponse> response) {
                UsuarioResponse cuerpo = response.body();
                if (!response.isSuccessful() || cuerpo == null) {
                    callback.onExito(publicacion);
                    return;
                }
                try {
                    callback.onExito(conVendedor(publicacion, aVendedor(cuerpo)));
                } catch (RuntimeException excepcion) {
                    callback.onExito(publicacion);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UsuarioResponse> call, @NonNull Throwable throwable) {
                callback.onExito(publicacion);
            }
        });
    }

    private static Vendedor aVendedor(UsuarioResponse usuario) {
        double reputacion = usuario.reputacion != null ? usuario.reputacion.promedio : 0.0;
        int cantidadVentas = usuario.reputacion != null ? usuario.reputacion.operacionesComoVendedor : 0;
        return new Vendedor(usuario.id, usuario.nombre, reputacion, cantidadVentas, usuario.fechaAlta);
    }

    private static Publicacion conVendedor(Publicacion original, Vendedor vendedor) {
        Publicacion actualizada = new Publicacion(original.getId(), original.getTitulo(), original.getDescripcion(),
                original.getPrecio(), original.getEstado(), original.getCategoria(), original.getZona(),
                original.getFechaPublicacion(), vendedor, original.getCantidadFotos(),
                original.getDireccionEntrega());
        actualizada.setEstadoPublicacion(original.getEstadoPublicacion());
        return actualizada;
    }

    @Override
    public void obtenerPerfilVendedor(String vendedorId, RepositorioCallback<PerfilVendedor> callback) {
        usuarioApi.obtenerPerfilPublico(vendedorId).enqueue(new Callback<UsuarioResponse>() {
            @Override
            public void onResponse(@NonNull Call<UsuarioResponse> call, @NonNull Response<UsuarioResponse> response) {
                UsuarioResponse cuerpo = response.body();
                if (!response.isSuccessful() || cuerpo == null) {
                    callback.onError(ErrorApi.mensaje(response, "No pudimos cargar el perfil"));
                    return;
                }
                Vendedor vendedor;
                try {
                    vendedor = aVendedor(cuerpo);
                } catch (RuntimeException excepcion) {
                    callback.onError("No pudimos cargar el perfil");
                    return;
                }
                cargarPublicacionesDelVendedor(vendedorId, 0, new ArrayList<>(), vendedor, callback);
            }

            @Override
            public void onFailure(@NonNull Call<UsuarioResponse> call, @NonNull Throwable throwable) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        });
    }

    private void cargarPublicacionesDelVendedor(String vendedorId, int pagina, List<Publicacion> acumuladas,
                                                Vendedor vendedor, RepositorioCallback<PerfilVendedor> callback) {
        FiltroPublicaciones sinFiltros = new FiltroPublicaciones();
        buscar(sinFiltros, vendedorId, pagina, new RepositorioCallback<PaginaPublicaciones>() {
            @Override
            public void onExito(PaginaPublicaciones resultado) {
                acumuladas.addAll(resultado.getPublicaciones());
                if (resultado.hayMas()) {
                    cargarPublicacionesDelVendedor(vendedorId, pagina + 1, acumuladas, vendedor, callback);
                } else {
                    callback.onExito(new PerfilVendedor(vendedor, acumuladas));
                }
            }

            @Override
            public void onError(String mensaje) {
                callback.onExito(new PerfilVendedor(vendedor, acumuladas));
            }
        });
    }

    @Override
    public void cambiarEstadoPublicacion(String id, EstadoPublicacion nuevoEstado,
                                         RepositorioCallback<Publicacion> callback) {
        CambiarEstadoPublicacionRequest request = new CambiarEstadoPublicacionRequest(nuevoEstado.name());
        api.cambiarEstado(id, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError(ErrorApi.mensaje(response, "No pudimos actualizar la publicación"));
                    return;
                }

                obtenerPublicacion(id, callback);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        });
    }

    @Override
    public void obtenerVarias(List<String> ids, RepositorioCallback<List<Publicacion>> callback) {
        if (ids.isEmpty()) {
            callback.onExito(new ArrayList<>());
            return;
        }
        Map<String, Publicacion> resultados = new ConcurrentHashMap<>();
        AtomicInteger pendientes = new AtomicInteger(ids.size());
        for (String id : ids) {
            obtenerPublicacion(id, new RepositorioCallback<Publicacion>() {
                @Override
                public void onExito(Publicacion publicacion) {
                    resultados.put(id, publicacion);
                    avisarSiTerminaron();
                }

                @Override
                public void onError(String mensaje) {
                    // Un id que ya no existe simplemente no aparece en el resultado.
                    avisarSiTerminaron();
                }

                private void avisarSiTerminaron() {
                    if (pendientes.decrementAndGet() != 0) {
                        return;
                    }
                    List<Publicacion> ordenado = new ArrayList<>();
                    for (String unId : ids) {
                        Publicacion publicacion = resultados.get(unId);
                        if (publicacion != null) {
                            ordenado.add(publicacion);
                        }
                    }
                    callback.onExito(ordenado);
                }
            });
        }
    }
}
