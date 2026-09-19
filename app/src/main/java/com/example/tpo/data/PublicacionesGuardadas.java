package com.example.tpo.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.tpo.data.local.AppDatabase;
import com.example.tpo.data.local.PublicacionGuardadaDao;
import com.example.tpo.data.local.PublicacionGuardadaEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Publicaciones que el usuario marcó como guardadas — Punto 4 del TPO.
 * <p>
 * Persistido con Room, mismo patrón que {@link MisPublicacionesRepositoryLocal} (Punto 5):
 * un DAO, un {@link ExecutorService} de un solo hilo para no tocar la base en el Main
 * Thread, y el resultado siempre entregado por {@link RepositorioCallback} en el Main
 * Thread. Contra la API real sería un {@code POST/DELETE /publicaciones/{id}/guardada}.
 * <p>
 * Se filtra por {@link SesionUsuario#getUsuarioId()}, igual que
 * {@link MisPublicacionesRepositoryLocal#listar}: sin esto, dos usuarios que probaran la
 * app en el mismo dispositivo verían los guardados del otro.
 * <p>
 * Se modela aparte de {@link SesionUsuario} a propósito: esa clase modela <em>quién</em>
 * es el usuario, mientras que las guardadas son <em>datos</em> suyos.
 */
public class PublicacionesGuardadas {

    private static PublicacionesGuardadas instancia;

    private final PublicacionGuardadaDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    private PublicacionesGuardadas(Context context) {
        dao = AppDatabase.getInstancia(context).publicacionGuardadaDao();
    }

    public static synchronized PublicacionesGuardadas getInstancia(Context context) {
        if (instancia == null) {
            instancia = new PublicacionesGuardadas(context.getApplicationContext());
        }
        return instancia;
    }

    public void estaGuardada(String publicacionId, RepositorioCallback<Boolean> callback) {
        String usuarioId = SesionUsuario.getInstancia().getUsuarioId();
        executor.execute(() -> {
            boolean guardada = dao.estaGuardada(usuarioId, publicacionId);
            handlerPrincipal.post(() -> callback.onExito(guardada));
        });
    }

    /**
     * Ids guardados por el usuario logueado, el más reciente primero — para la pantalla
     * "Guardados". Devuelve solo ids (no el objeto {@link com.example.tpo.model.Publicacion}
     * completo) a propósito: esos datos ya viven en el catálogo de
     * {@code PublicacionRepositoryMock} y hay que pedírselo a él para no mostrar una copia
     * vieja (precio, estado) — ver {@code PublicacionRepository#obtenerVarias}.
     */
    public void listar(RepositorioCallback<List<String>> callback) {
        String usuarioId = SesionUsuario.getInstancia().getUsuarioId();
        executor.execute(() -> {
            List<String> ids = dao.listarIds(usuarioId);
            handlerPrincipal.post(() -> callback.onExito(ids));
        });
    }

    /**
     * Alterna el estado de una publicación. El {@code Boolean} que recibe
     * {@link RepositorioCallback#onExito} es el estado nuevo: {@code true} si quedó
     * guardada, {@code false} si se quitó.
     */
    public void alternar(String publicacionId, RepositorioCallback<Boolean> callback) {
        String usuarioId = SesionUsuario.getInstancia().getUsuarioId();
        executor.execute(() -> {
            boolean estabaGuardada = dao.estaGuardada(usuarioId, publicacionId);
            if (estabaGuardada) {
                dao.quitar(usuarioId, publicacionId);
            } else {
                PublicacionGuardadaEntity entity = new PublicacionGuardadaEntity();
                entity.usuarioId = usuarioId;
                entity.publicacionId = publicacionId;
                entity.fechaGuardado = System.currentTimeMillis();
                dao.guardar(entity);
            }
            boolean quedoGuardada = !estabaGuardada;
            handlerPrincipal.post(() -> callback.onExito(quedoGuardada));
        });
    }
}
