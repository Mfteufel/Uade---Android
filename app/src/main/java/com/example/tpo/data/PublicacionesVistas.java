package com.example.tpo.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.tpo.data.local.AppDatabase;
import com.example.tpo.data.local.PublicacionVistaDao;
import com.example.tpo.data.local.PublicacionVistaEntity;
import com.example.tpo.model.Publicacion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Últimas publicaciones que el usuario vio — Punto 6 del TPO (modo sin
 * conexión).
 */
public class PublicacionesVistas {

    /** Cuántas publicaciones vistas se guardan como máximo por usuario. */
    private static final int LIMITE = 10;

    private static PublicacionesVistas instancia;

    private final PublicacionVistaDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    private PublicacionesVistas(Context context) {
        dao = AppDatabase.getInstancia(context).publicacionVistaDao();
    }

    public static synchronized PublicacionesVistas getInstancia(Context context) {
        if (instancia == null) {
            instancia = new PublicacionesVistas(context.getApplicationContext());
        }
        return instancia;
    }

    /** Guarda la publicación tocada como "vista". Fire-and-forget: no hay nada que reportarle a la UI. */
    public void registrarVista(Publicacion publicacion) {
        String usuarioId = SesionUsuario.getInstancia().getUsuarioId();
        long ahora = System.currentTimeMillis();
        executor.execute(() -> {
            dao.guardar(PublicacionVistaEntity.desde(usuarioId, publicacion, ahora));
            dao.limitarCantidad(usuarioId, LIMITE);
        });
    }

    /** Últimas publicaciones vistas por el usuario logueado, la más reciente primero. */
    public void listar(RepositorioCallback<List<Publicacion>> callback) {
        String usuarioId = SesionUsuario.getInstancia().getUsuarioId();
        executor.execute(() -> {
            List<Publicacion> publicaciones = new ArrayList<>();
            for (PublicacionVistaEntity entidad : dao.obtenerTodas(usuarioId)) {
                publicaciones.add(entidad.aPublicacion());
            }
            handlerPrincipal.post(() -> callback.onExito(publicaciones));
        });
    }

    public void obtenerVista(String publicacionId, RepositorioCallback<Publicacion> callback) {
        String usuarioId = SesionUsuario.getInstancia().getUsuarioId();
        executor.execute(() -> {
            PublicacionVistaEntity entidad = dao.obtenerPorId(usuarioId, publicacionId);
            if (entidad == null) {
                handlerPrincipal.post(() -> callback.onError("No tenés esta publicación guardada"));
                return;
            }
            handlerPrincipal.post(() -> callback.onExito(entidad.aPublicacion()));
        });
    }
}
