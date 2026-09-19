package com.example.tpo.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.tpo.data.local.AppDatabase;
import com.example.tpo.data.local.PreguntaDao;
import com.example.tpo.data.local.PreguntaEntity;
import com.example.tpo.model.Pregunta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Preguntas que los interesados le hicieron a los vendedores — Punto 4 del TPO.
 * <p>
 * Persistido con Room, mismo patrón que {@link MisPublicacionesRepositoryLocal} (Punto 5):
 * un DAO, un {@link ExecutorService} de un solo hilo para no tocar la base en el Main
 * Thread, y el resultado siempre entregado por {@link RepositorioCallback} en el Main
 * Thread. Contra la API real cada pregunta sale de un
 * {@code POST /publicaciones/{id}/preguntas}.
 * <p>
 * A diferencia de {@link OfertasPublicacion}, acá no hay límite: un mismo usuario puede
 * mandar varias preguntas sobre la misma publicación, es lo normal en una conversación.
 * No hace falta filtrar por usuario logueado como en {@link PublicacionesGuardadas}: cada
 * fila ya lleva su propio {@code autorId}.
 */
public class PreguntasPublicacion {

    private static PreguntasPublicacion instancia;

    private final PreguntaDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    private PreguntasPublicacion(Context context) {
        dao = AppDatabase.getInstancia(context).preguntaDao();
    }

    public static synchronized PreguntasPublicacion getInstancia(Context context) {
        if (instancia == null) {
            instancia = new PreguntasPublicacion(context.getApplicationContext());
        }
        return instancia;
    }

    public void agregar(Pregunta pregunta, RepositorioCallback<Void> callback) {
        executor.execute(() -> {
            PreguntaEntity entity = new PreguntaEntity();
            entity.publicacionId = pregunta.getPublicacionId();
            entity.autorId = pregunta.getAutorId();
            entity.autorNombre = pregunta.getAutorNombre();
            entity.texto = pregunta.getTexto();
            entity.fecha = pregunta.getFecha();
            dao.agregar(entity);
            handlerPrincipal.post(() -> callback.onExito(null));
        });
    }

    /** Todas las preguntas de una publicación, para que las vea su vendedor en la gestión. */
    public void deLaPublicacion(String publicacionId, RepositorioCallback<List<Pregunta>> callback) {
        executor.execute(() -> {
            List<Pregunta> resultado = haciaModelo(dao.deLaPublicacion(publicacionId));
            handlerPrincipal.post(() -> callback.onExito(resultado));
        });
    }

    /** Solo las preguntas de un usuario puntual, para que el Detalle muestre "lo que enviaste". */
    public void delUsuario(String publicacionId, String usuarioId,
                           RepositorioCallback<List<Pregunta>> callback) {
        executor.execute(() -> {
            List<Pregunta> resultado = haciaModelo(dao.delUsuario(publicacionId, usuarioId));
            handlerPrincipal.post(() -> callback.onExito(resultado));
        });
    }

    private static List<Pregunta> haciaModelo(List<PreguntaEntity> entidades) {
        List<Pregunta> resultado = new ArrayList<>();
        for (PreguntaEntity entity : entidades) {
            resultado.add(new Pregunta(
                    entity.publicacionId, entity.autorId, entity.autorNombre,
                    entity.texto, entity.fecha));
        }
        return resultado;
    }
}
