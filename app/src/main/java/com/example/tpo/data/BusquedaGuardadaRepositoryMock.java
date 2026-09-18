package com.example.tpo.data;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.tpo.model.BusquedaGuardada;
import com.example.tpo.model.Categoria;
import com.example.tpo.model.FiltroPublicaciones;
import com.example.tpo.model.Publicacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BusquedaGuardadaRepositoryMock implements BusquedaGuardadaRepository {

    private static final long DEMORA_SIMULADA_MS = 300;

    private static final boolean SIMULAR_ERROR = false;

    private static BusquedaGuardadaRepositoryMock instancia;

    private final Map<String, BusquedaGuardada> busquedas = new LinkedHashMap<>();
    private final Map<String, Set<String>> publicacionesNuevasPorBusqueda = new HashMap<>();
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    private BusquedaGuardadaRepositoryMock() {
        // Constructor privado: se accede siempre por getInstancia().
    }

    public static synchronized BusquedaGuardadaRepositoryMock getInstancia() {
        if (instancia == null) {
            instancia = new BusquedaGuardadaRepositoryMock();
        }
        return instancia;
    }

    @Override
    public void guardar(String nombre, FiltroPublicaciones filtro, RepositorioCallback<Void> callback) {
        handlerPrincipal.postDelayed(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos guardar la búsqueda");
                return;
            }
            if (yaExiste(filtro)) {
                callback.onError("Ya tenés guardada una búsqueda con estos mismos filtros");
                return;
            }
            BusquedaGuardada nueva = new BusquedaGuardada(nombre, filtro);
            busquedas.put(nueva.getId(), nueva);
            callback.onExito(null);
        }, DEMORA_SIMULADA_MS);
    }

    /**
     * true si ya hay una búsqueda guardada con exactamente los mismos
     * criterios para evitar duplicados
     */
    private boolean yaExiste(FiltroPublicaciones filtro) {
        for (BusquedaGuardada existente : busquedas.values()) {
            if (existente.getFiltro().equals(filtro)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void listar(RepositorioCallback<List<BusquedaGuardada>> callback) {
        handlerPrincipal.postDelayed(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos cargar tus búsquedas guardadas");
                return;
            }
            List<BusquedaGuardada> resultado = new ArrayList<>(busquedas.values());
            // La más recién guardada primero: es el orden más útil para reencontrarla.
            Collections.reverse(resultado);
            callback.onExito(resultado);
        }, DEMORA_SIMULADA_MS);
    }

    @Override
    public void eliminar(String id, RepositorioCallback<Void> callback) {
        handlerPrincipal.postDelayed(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos eliminar la búsqueda");
                return;
            }
            busquedas.remove(id);
            publicacionesNuevasPorBusqueda.remove(id);
            callback.onExito(null);
        }, DEMORA_SIMULADA_MS);
    }

    @Override
    public boolean tieneNovedad(String id) {
        Set<String> nuevas = publicacionesNuevasPorBusqueda.get(id);
        return nuevas != null && !nuevas.isEmpty();
    }

    @Override
    public boolean hayAlgunaNovedad() {
        return !publicacionesNuevasPorBusqueda.isEmpty();
    }

    @Override
    public void marcarTodoVisto() {
        publicacionesNuevasPorBusqueda.clear();
    }

    @Override
    public Set<String> publicacionesNuevasDe(String id) {
        Set<String> nuevas = publicacionesNuevasPorBusqueda.get(id);
        return nuevas != null ? nuevas : Collections.emptySet();
    }

    /**
     * Agrega una publicación de prueba (con título y categoría opcionales,
     * para poder matchear la búsqueda guardada que se esté probando) y marca
     * con novedad cada búsqueda guardada que matchee, guardando qué
     * publicación puntual fue. Se dispara vía ADB.
     */
    public void simularPublicacionNueva(@Nullable String titulo, @Nullable Categoria categoria) {
        Publicacion nueva = PublicacionRepositoryMock.getInstancia().agregarPublicacionDePrueba(titulo, categoria);
        for (BusquedaGuardada busqueda : busquedas.values()) {
            if (PublicacionRepositoryMock.getInstancia().coincideConFiltro(nueva, busqueda.getFiltro())) {
                publicacionesNuevasPorBusqueda
                        .computeIfAbsent(busqueda.getId(), id -> new HashSet<>())
                        .add(nueva.getId());
            }
        }
    }
}
