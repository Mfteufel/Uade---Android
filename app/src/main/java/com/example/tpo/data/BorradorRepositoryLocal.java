package com.example.tpo.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.tpo.data.local.AppDatabase;
import com.example.tpo.data.local.BorradorPublicacionDao;
import com.example.tpo.data.local.BorradorPublicacionEntity;
import com.example.tpo.model.BorradorPublicacion;
import com.example.tpo.model.Categoria;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.Zona;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementación de {@link BorradorRepository} con Room.
 * <p>
 * Room prohíbe correr queries en el Main Thread, así que todo se despacha a un
 * {@link ExecutorService} de un solo hilo (alcanza: nunca hay dos operaciones
 * de borrador en simultáneo) y el resultado se postea de vuelta al Main Thread
 * con un {@link Handler}, igual que hace {@code PublicacionRepositoryMock} con
 * su demora simulada.
 */
public class BorradorRepositoryLocal implements BorradorRepository {

    private static BorradorRepositoryLocal instancia;

    private final BorradorPublicacionDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    private BorradorRepositoryLocal(Context context) {
        dao = AppDatabase.getInstancia(context).borradorPublicacionDao();
    }

    public static synchronized BorradorRepositoryLocal getInstancia(Context context) {
        if (instancia == null) {
            instancia = new BorradorRepositoryLocal(context.getApplicationContext());
        }
        return instancia;
    }

    @Override
    public void guardar(BorradorPublicacion borrador) {
        BorradorPublicacionEntity entity = haciaEntity(borrador);
        executor.execute(() -> dao.guardar(entity));
    }

    @Override
    public void obtener(RepositorioCallback<BorradorPublicacion> callback) {
        executor.execute(() -> {
            BorradorPublicacionEntity entity = dao.obtener();
            BorradorPublicacion borrador = entity == null ? null : desdeEntity(entity);
            handlerPrincipal.post(() -> callback.onExito(borrador));
        });
    }

    @Override
    public void borrar() {
        executor.execute(dao::borrar);
    }

    // ---------------------------------------------------------------------
    // Mapeo entidad <-> modelo de dominio
    // ---------------------------------------------------------------------

    private static BorradorPublicacionEntity haciaEntity(BorradorPublicacion borrador) {
        BorradorPublicacionEntity entity = new BorradorPublicacionEntity();
        entity.fotos = borrador.getFotos();
        entity.titulo = borrador.getTitulo();
        entity.descripcion = borrador.getDescripcion();
        entity.categoria = nombreOrNull(borrador.getCategoria());
        entity.estadoArticulo = nombreOrNull(borrador.getEstadoArticulo());
        entity.precio = borrador.getPrecio();
        entity.zona = nombreOrNull(borrador.getZona());
        entity.paso = borrador.getPaso();
        entity.actualizadoEn = System.currentTimeMillis();
        return entity;
    }

    private static BorradorPublicacion desdeEntity(BorradorPublicacionEntity entity) {
        BorradorPublicacion borrador = new BorradorPublicacion();
        borrador.setFotos(entity.fotos);
        borrador.setTitulo(entity.titulo);
        borrador.setDescripcion(entity.descripcion);
        borrador.setCategoria(valorEnumOrNull(Categoria.class, entity.categoria));
        borrador.setEstadoArticulo(valorEnumOrNull(EstadoArticulo.class, entity.estadoArticulo));
        borrador.setPrecio(entity.precio);
        borrador.setZona(valorEnumOrNull(Zona.class, entity.zona));
        borrador.setPaso(entity.paso);
        return borrador;
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
            // El borrador quedó guardado con un valor que ya no existe en el enum
            // (por ejemplo, se sacó una categoría). Se descarta ese campo puntual
            // en vez de romper la carga de todo el borrador.
            return null;
        }
    }
}
