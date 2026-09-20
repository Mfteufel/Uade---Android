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
 * <p>
 * Mismo patrón que {@link PublicacionesGuardadas}: un DAO, un
 * {@link ExecutorService} de un solo hilo para no tocar la base en el Main
 * Thread, resultado siempre entregado por {@link RepositorioCallback} en el
 * Main Thread. Se filtra por {@link SesionUsuario#getUsuarioId()}, mismo
 * motivo que {@link PublicacionesGuardadas}: no mezclar el historial de dos
 * usuarios que prueban la app en el mismo dispositivo.
 * <p>
 * A diferencia de {@link PublicacionesGuardadas} (que solo guarda el id
 * porque siempre puede volver a pedirle los datos actuales a
 * {@code PublicacionRepositoryMock}), acá se guarda una copia completa de la
 * publicación: sin conexión no hay forma de volver a pedírsela a nadie.
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
}
