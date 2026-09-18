package com.example.tpo.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.tpo.data.local.AppDatabase;
import com.example.tpo.data.local.MiPublicacionDao;
import com.example.tpo.data.local.PublicacionMiaEntity;
import com.example.tpo.model.BorradorPublicacion;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.MiPublicacion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementación de {@link MisPublicacionesRepository} con Room.
 * <p>
 * Reemplazo temporal de {@link MisPublicacionesRepositoryApi} mientras no
 * existe un backend real: persiste todo en el dispositivo, filtrado por
 * {@link SesionUsuario#getUsuarioId()}, así que sirve para demostrar el ciclo
 * completo (publicar → ver en "Mis publicaciones" → pausar/reactivar) cambiando
 * de usuario en el mismo dispositivo. No resuelve que dos instalaciones
 * distintas de la app se vean entre sí — eso necesita sí o sí un servidor
 * compartido en la red, como el que se está armando con FastAPI.
 * <p>
 * El día que ese backend esté listo, alcanza con volver a instanciar
 * {@link MisPublicacionesRepositoryApi} en su lugar (ver
 * {@code PublicarArticuloViewModel} y {@code MisPublicacionesFragment}): la
 * interfaz no cambia.
 */
public class MisPublicacionesRepositoryLocal implements MisPublicacionesRepository {

    private static MisPublicacionesRepositoryLocal instancia;

    private final MiPublicacionDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    private MisPublicacionesRepositoryLocal(Context context) {
        dao = AppDatabase.getInstancia(context).miPublicacionDao();
    }

    public static synchronized MisPublicacionesRepositoryLocal getInstancia(Context context) {
        if (instancia == null) {
            instancia = new MisPublicacionesRepositoryLocal(context.getApplicationContext());
        }
        return instancia;
    }

    @Override
    public void publicar(BorradorPublicacion borrador, RepositorioCallback<MiPublicacion> callback) {
        executor.execute(() -> {
            PublicacionMiaEntity entity = new PublicacionMiaEntity();
            entity.id = UUID.randomUUID().toString();
            entity.vendedorId = SesionUsuario.getInstancia().getUsuarioId();
            entity.titulo = borrador.getTitulo();
            entity.descripcion = borrador.getDescripcion();
            entity.categoria = nombreOrNull(borrador.getCategoria());
            entity.estadoArticulo = nombreOrNull(borrador.getEstadoArticulo());
            entity.precio = borrador.getPrecio() != null ? borrador.getPrecio() : 0;
            entity.zona = nombreOrNull(borrador.getZona());
            entity.fotos = new ArrayList<>(borrador.getFotos());
            entity.estadoPublicacion = EstadoPublicacion.ACTIVA.name();
            entity.fechaPublicacion = System.currentTimeMillis();

            dao.insertar(entity);
            MiPublicacion creada = haciaModelo(entity);
            handlerPrincipal.post(() -> callback.onExito(creada));
        });
    }

    @Override
    public void listar(RepositorioCallback<List<MiPublicacion>> callback) {
        executor.execute(() -> {
            List<PublicacionMiaEntity> entidades =
                    dao.listarPorVendedor(SesionUsuario.getInstancia().getUsuarioId());
            List<MiPublicacion> resultado = new ArrayList<>();
            for (PublicacionMiaEntity entity : entidades) {
                resultado.add(haciaModelo(entity));
            }
            handlerPrincipal.post(() -> callback.onExito(resultado));
        });
    }

    @Override
    public void pausar(String idPublicacion, RepositorioCallback<Void> callback) {
        cambiarEstado(idPublicacion, EstadoPublicacion.PAUSADA, callback);
    }

    @Override
    public void reactivar(String idPublicacion, RepositorioCallback<Void> callback) {
        cambiarEstado(idPublicacion, EstadoPublicacion.ACTIVA, callback);
    }

    private void cambiarEstado(String idPublicacion, EstadoPublicacion nuevoEstado,
                               RepositorioCallback<Void> callback) {
        executor.execute(() -> {
            dao.actualizarEstado(idPublicacion, nuevoEstado.name());
            handlerPrincipal.post(() -> callback.onExito(null));
        });
    }

    private static MiPublicacion haciaModelo(PublicacionMiaEntity entity) {
        String fotoPrincipalUrl = entity.fotos.isEmpty() ? null : entity.fotos.get(0).toString();
        return new MiPublicacion(
                entity.id,
                entity.titulo,
                entity.precio,
                fotoPrincipalUrl,
                valorEnumOrNull(EstadoArticulo.class, entity.estadoArticulo),
                EstadoPublicacion.valueOf(entity.estadoPublicacion),
                entity.fechaPublicacion);
    }

    @Nullable
    private static String nombreOrNull(@Nullable Enum<?> valor) {
        return valor == null ? null : valor.name();
    }

    @Nullable
    private static <T extends Enum<T>> T valorEnumOrNull(Class<T> tipo, @Nullable String nombre) {
        if (nombre == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, nombre);
        } catch (IllegalArgumentException excepcion) {
            return null;
        }
    }
}
