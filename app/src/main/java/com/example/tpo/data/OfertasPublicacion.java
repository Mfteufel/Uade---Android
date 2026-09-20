package com.example.tpo.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.tpo.data.local.AppDatabase;
import com.example.tpo.data.local.OfertaDao;
import com.example.tpo.data.local.OfertaEntity;
import com.example.tpo.model.Oferta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ofertas de precio que los interesados le hicieron a los vendedores — Punto 4 del TPO.
 * <p>
 * Persistido con Room, mismo patrón que {@link MisPublicacionesRepositoryLocal} (Punto 5):
 * un DAO, un {@link ExecutorService} de un solo hilo para no tocar la base en el Main
 * Thread, y el resultado siempre entregado por {@link RepositorioCallback} en el Main
 * Thread. Contra la API real sería un {@code POST /publicaciones/{id}/ofertas}.
 * <p>
 * <b>Regla propia de esta clase:</b> cada usuario tiene como máximo <em>una</em> oferta
 * vigente por publicación. Volver a ofertar no apila una oferta nueva, reemplaza la
 * anterior — eso ahora lo garantiza la clave primaria compuesta de {@link OfertaEntity}
 * ({@code publicacionId} + {@code autorId}) junto con {@code OnConflictStrategy.REPLACE}
 * en {@link OfertaDao#guardar}, no una comparación manual como antes.
 */
public class OfertasPublicacion {

    private static OfertasPublicacion instancia;

    private final OfertaDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    private OfertasPublicacion(Context context) {
        dao = AppDatabase.getInstancia(context).ofertaDao();
    }

    public static synchronized OfertasPublicacion getInstancia(Context context) {
        if (instancia == null) {
            instancia = new OfertasPublicacion(context.getApplicationContext());
        }
        return instancia;
    }

    /** Guarda la oferta, reemplazando la anterior del mismo usuario para esa publicación si existía. */
    public void guardar(Oferta oferta, RepositorioCallback<Void> callback) {
        executor.execute(() -> {
            OfertaEntity entity = new OfertaEntity();
            entity.publicacionId = oferta.getPublicacionId();
            entity.autorId = oferta.getAutorId();
            entity.autorNombre = oferta.getAutorNombre();
            entity.monto = oferta.getMonto();
            entity.fecha = oferta.getFecha();
            dao.guardar(entity);
            handlerPrincipal.post(() -> callback.onExito(null));
        });
    }

    /** Todas las ofertas de una publicación, para que las vea su vendedor en la gestión. */
    public void deLaPublicacion(String publicacionId, RepositorioCallback<List<Oferta>> callback) {
        executor.execute(() -> {
            List<Oferta> resultado = new ArrayList<>();
            for (OfertaEntity entity : dao.deLaPublicacion(publicacionId)) {
                resultado.add(haciaModelo(entity));
            }
            handlerPrincipal.post(() -> callback.onExito(resultado));
        });
    }

    /** La oferta vigente de un usuario para una publicación, o {@code null} si no hizo ninguna. */
    public void delUsuario(String publicacionId, String usuarioId,
                           RepositorioCallback<Oferta> callback) {
        executor.execute(() -> {
            OfertaEntity entity = dao.delUsuario(publicacionId, usuarioId);
            Oferta resultado = entity == null ? null : haciaModelo(entity);
            handlerPrincipal.post(() -> callback.onExito(resultado));
        });
    }

    private static Oferta haciaModelo(OfertaEntity entity) {
        return new Oferta(
                entity.publicacionId, entity.autorId, entity.autorNombre,
                entity.monto, entity.fecha);
    }
}
