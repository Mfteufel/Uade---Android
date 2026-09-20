package com.example.tpo.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Punto 6 (modo sin conexión): acceso a las publicaciones vistas cacheadas.
 * Métodos síncronos, igual que el resto de los DAO del proyecto: quien llame
 * corre en un hilo de fondo (ver {@code PublicacionesVistas}).
 */
@Dao
public interface PublicacionVistaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(PublicacionVistaEntity publicacion);

    @Query("SELECT * FROM publicaciones_vistas WHERE usuarioId = :usuarioId ORDER BY guardadoEn DESC")
    List<PublicacionVistaEntity> obtenerTodas(String usuarioId);

    /**
     * Se queda con las {@code limite} más recientes del usuario y borra el
     * resto. Se llama siempre después de {@link #guardar}, así el cache actúa
     * como un LRU acotado por usuario en vez de crecer indefinidamente.
     */
    @Query("DELETE FROM publicaciones_vistas WHERE usuarioId = :usuarioId AND id NOT IN "
            + "(SELECT id FROM publicaciones_vistas WHERE usuarioId = :usuarioId ORDER BY guardadoEn DESC LIMIT :limite)")
    void limitarCantidad(String usuarioId, int limite);
}
