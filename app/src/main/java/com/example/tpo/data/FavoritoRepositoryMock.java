package com.example.tpo.data;

import android.os.Handler;
import android.os.Looper;

import com.example.tpo.model.Publicacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FavoritoRepositoryMock implements FavoritoRepository {

    private static final long DEMORA_SIMULADA_MS = 300;

    private static final boolean SIMULAR_ERROR = false;

    private static FavoritoRepositoryMock instancia;

    private final Map<String, Map<String, Publicacion>> favoritosPorUsuario = new HashMap<>();
    private final Map<String, Set<String>> conNovedadPorUsuario = new HashMap<>();
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    private FavoritoRepositoryMock() {
        // Constructor privado: se accede siempre por getInstancia().
    }

    public static synchronized FavoritoRepositoryMock getInstancia() {
        if (instancia == null) {
            instancia = new FavoritoRepositoryMock();
        }
        return instancia;
    }

    @Override
    public void precargar(RepositorioCallback<Void> callback) {
        handlerPrincipal.post(() -> callback.onExito(null));
    }

    private Map<String, Publicacion> favoritosDelUsuario() {
        String usuarioId = SesionUsuario.getInstancia().getUsuarioId();
        return favoritosPorUsuario.computeIfAbsent(usuarioId, id -> new LinkedHashMap<>());
    }

    private Set<String> conNovedadDelUsuario() {
        String usuarioId = SesionUsuario.getInstancia().getUsuarioId();
        return conNovedadPorUsuario.computeIfAbsent(usuarioId, id -> new HashSet<>());
    }

    @Override
    public boolean esFavorito(String publicacionId) {
        return favoritosDelUsuario().containsKey(publicacionId);
    }

    @Override
    public void marcar(Publicacion publicacion, RepositorioCallback<Void> callback) {
        Map<String, Publicacion> favoritos = favoritosDelUsuario();
        handlerPrincipal.postDelayed(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos guardar el favorito");
                return;
            }
            favoritos.put(publicacion.getId(), publicacion);
            callback.onExito(null);
        }, DEMORA_SIMULADA_MS);
    }

    @Override
    public void desmarcar(String publicacionId, RepositorioCallback<Void> callback) {
        Map<String, Publicacion> favoritos = favoritosDelUsuario();
        Set<String> conNovedad = conNovedadDelUsuario();
        handlerPrincipal.postDelayed(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos quitar el favorito");
                return;
            }
            favoritos.remove(publicacionId);
            conNovedad.remove(publicacionId);
            callback.onExito(null);
        }, DEMORA_SIMULADA_MS);
    }

    @Override
    public void listar(RepositorioCallback<List<Publicacion>> callback) {
        Map<String, Publicacion> favoritos = favoritosDelUsuario();
        handlerPrincipal.postDelayed(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos cargar tus favoritos");
                return;
            }
            List<Publicacion> resultado = new ArrayList<>(favoritos.values());
            // El más recién marcado primero: es el orden más útil para revisitar.
            Collections.reverse(resultado);
            callback.onExito(resultado);
        }, DEMORA_SIMULADA_MS);
    }

    @Override
    public boolean tieneNovedad(String publicacionId) {
        return conNovedadDelUsuario().contains(publicacionId);
    }

    @Override
    public boolean hayAlgunaNovedad() {
        return !conNovedadDelUsuario().isEmpty();
    }

    @Override
    public void marcarTodoVisto() {
        conNovedadDelUsuario().clear();
    }

    /**
     * Simula una baja de precio en el favorito marcado más recientemente del usuario
     * logueado, para poder probar el indicador de novedad sin backend. Se dispara vía ADB.
     */
    public void simularCambioDePrecio() {
        Map<String, Publicacion> favoritos = favoritosDelUsuario();
        if (favoritos.isEmpty()) {
            return;
        }
        String idMasReciente = null;
        for (String id : favoritos.keySet()) {
            idMasReciente = id;
        }
        Publicacion publicacion = favoritos.get(idMasReciente);
        publicacion.actualizarPrecio(publicacion.getPrecio() * 0.85);
        conNovedadDelUsuario().add(idMasReciente);
    }
}
