package com.example.tpo.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.tpo.data.local.AppDatabase;
import com.example.tpo.data.local.OfertaDao;
import com.example.tpo.data.local.OfertaEntity;
import com.example.tpo.model.EstadoOferta;
import com.example.tpo.model.Oferta;
import com.example.tpo.model.Publicacion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Ofertas de precio que los interesados le hicieron a los vendedores — Punto 4 del TPO,
 * con negociación completa (aceptar / rechazar / contraofertar / vencimiento) agregada
 * en el Punto 7.
 * <p>
 * Persistido con Room, mismo patrón que {@link MisPublicacionesRepositoryLocal} (Punto 5):
 * un DAO, un {@link ExecutorService} de un solo hilo para no tocar la base en el Main
 * Thread, y el resultado siempre entregado por {@link RepositorioCallback} en el Main
 * Thread. Contra la API real sería un {@code POST /publicaciones/{id}/ofertas}.
 * <p>
 * <b>Regla propia de esta clase:</b> cada usuario tiene como máximo <em>una</em> oferta
 * vigente por publicación. Volver a ofertar no apila una oferta nueva, reemplaza la
 * anterior — eso lo garantiza la clave primaria compuesta de {@link OfertaEntity}
 * ({@code publicacionId} + {@code autorId}) junto con {@code OnConflictStrategy.REPLACE}
 * en {@link OfertaDao#guardar}, no una comparación manual como antes.
 */
public class OfertasPublicacion {

    /** Cuánto dura una oferta (o contraoferta) antes de vencer sola si nadie responde. */
    private static final long DURACION_OFERTA_MS = TimeUnit.HOURS.toMillis(48);

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

    /**
     * Arma una oferta nueva contra una publicación puntual, con la duración y el estado
     * inicial que le corresponden a toda oferta recién creada. Centraliza acá esta regla
     * de negocio (y no en el Fragment) por el mismo motivo que el filtrado vive en
     * {@code PublicacionRepositoryMock}: es responsabilidad que en la app final tendría
     * el backend.
     */
    public Oferta crearOferta(Publicacion publicacion, String compradorId, String compradorNombre, double monto) {
        long ahora = System.currentTimeMillis();
        return new Oferta(
                UUID.randomUUID().toString(), publicacion.getId(), compradorId, compradorNombre,
                publicacion.getVendedor().getId(), monto, ahora, ahora + DURACION_OFERTA_MS,
                EstadoOferta.PENDIENTE, compradorId, null);
    }

    /** Guarda la oferta, reemplazando la anterior del mismo usuario para esa publicación si existía. */
    public void guardar(Oferta oferta, RepositorioCallback<Void> callback) {
        executor.execute(() -> {
            dao.guardar(haciaEntity(oferta));
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

    /** Una oferta puntual por su id propio — la usa el Detalle de oferta (Punto 7). */
    public void obtenerPorId(String ofertaId, RepositorioCallback<Oferta> callback) {
        executor.execute(() -> {
            OfertaEntity entity = dao.porId(ofertaId);
            if (entity == null) {
                handlerPrincipal.post(() -> callback.onError("No encontramos esta oferta"));
                return;
            }
            Oferta resultado = haciaModelo(entity);
            handlerPrincipal.post(() -> callback.onExito(resultado));
        });
    }

    /** El vendedor acepta la oferta: pasa a ACEPTADA. */
    public void aceptar(String ofertaId, RepositorioCallback<Void> callback) {
        executor.execute(() -> {
            dao.actualizarEstado(ofertaId, EstadoOferta.ACEPTADA.name());
            handlerPrincipal.post(() -> callback.onExito(null));
        });
    }

    /** El vendedor (o el comprador, sobre una contraoferta) rechaza: pasa a RECHAZADA, terminal. */
    public void rechazar(String ofertaId, RepositorioCallback<Void> callback) {
        executor.execute(() -> {
            dao.actualizarEstado(ofertaId, EstadoOferta.RECHAZADA.name());
            handlerPrincipal.post(() -> callback.onExito(null));
        });
    }

    /**
     * Contraoferta de cualquiera de las dos partes: actualiza el último monto propuesto
     * y quién lo propuso, y reabre la negociación (siempre vuelve a {@code PENDIENTE}).
     */
    public void contraofertar(String ofertaId, double nuevoMonto, String propuestoPor,
                              @Nullable String mensaje, RepositorioCallback<Void> callback) {
        executor.execute(() -> {
            dao.contraofertar(ofertaId, nuevoMonto, propuestoPor, mensaje, EstadoOferta.PENDIENTE.name());
            handlerPrincipal.post(() -> callback.onExito(null));
        });
    }

    /** Ofertas que el usuario hizo como comprador — pestaña "Enviadas" de "Mis ofertas". */
    public void misOfertasComoComprador(String usuarioId, RepositorioCallback<List<Oferta>> callback) {
        executor.execute(() -> {
            List<Oferta> resultado = new ArrayList<>();
            for (OfertaEntity entity : dao.comoComprador(usuarioId)) {
                resultado.add(haciaModelo(entity));
            }
            handlerPrincipal.post(() -> callback.onExito(resultado));
        });
    }

    /** Ofertas que el usuario recibió como vendedor — pestaña "Recibidas" de "Mis ofertas". */
    public void misOfertasComoVendedor(String usuarioId, RepositorioCallback<List<Oferta>> callback) {
        executor.execute(() -> {
            List<Oferta> resultado = new ArrayList<>();
            for (OfertaEntity entity : dao.comoVendedor(usuarioId)) {
                resultado.add(haciaModelo(entity));
            }
            handlerPrincipal.post(() -> callback.onExito(resultado));
        });
    }

    /**
     * Pasa a VENCIDA toda oferta PENDIENTE cuya fecha de vencimiento ya pasó. Se llama al
     * entrar a "Mis ofertas"; no hace falta WorkManager para esto todavía.
     */
    public void marcarVencidas(RepositorioCallback<Void> callback) {
        executor.execute(() -> {
            dao.marcarVencidas(EstadoOferta.PENDIENTE.name(), EstadoOferta.VENCIDA.name(),
                    System.currentTimeMillis());
            handlerPrincipal.post(() -> callback.onExito(null));
        });
    }

    /**
     * true si el usuario tiene una oferta ACEPTADA para esa publicación — el método
     * "claro" que necesita el Detalle de Publicación para decidir si muestra
     * {@link Publicacion#getDireccionEntrega()} o no.
     */
    public void tieneOfertaAceptada(String publicacionId, String usuarioId,
                                    RepositorioCallback<Boolean> callback) {
        executor.execute(() -> {
            OfertaEntity entity = dao.delUsuario(publicacionId, usuarioId);
            boolean aceptada = entity != null && EstadoOferta.ACEPTADA.name().equals(entity.estado);
            handlerPrincipal.post(() -> callback.onExito(aceptada));
        });
    }

    private static OfertaEntity haciaEntity(Oferta oferta) {
        OfertaEntity entity = new OfertaEntity();
        entity.id = oferta.getId();
        entity.publicacionId = oferta.getPublicacionId();
        entity.autorId = oferta.getAutorId();
        entity.autorNombre = oferta.getAutorNombre();
        entity.vendedorId = oferta.getVendedorId();
        entity.monto = oferta.getMonto();
        entity.fecha = oferta.getFecha();
        entity.fechaVencimiento = oferta.getFechaVencimiento();
        entity.estado = oferta.getEstado().name();
        entity.propuestoPor = oferta.getPropuestoPor();
        entity.mensaje = oferta.getMensaje();
        return entity;
    }

    private static Oferta haciaModelo(OfertaEntity entity) {
        return new Oferta(
                entity.id, entity.publicacionId, entity.autorId, entity.autorNombre,
                entity.vendedorId, entity.monto, entity.fecha, entity.fechaVencimiento,
                EstadoOferta.valueOf(entity.estado), entity.propuestoPor, entity.mensaje);
    }
}
