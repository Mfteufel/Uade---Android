package com.example.tpo.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Acceso a las publicaciones vistas cacheadas (modo offline)
 */
@Dao
public interface PublicacionVistaDao {

    /** Máximo de publicaciones vistas que guardamos en la cache */
    int LIMITE = 10;

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(PublicacionVistaEntity publicacion);

    @Query("SELECT * FROM publicaciones_vistas ORDER BY guardado_en DESC")
    List<PublicacionVistaEntity> obtenerTodas();

    @Query("DELETE FROM publicaciones_vistas WHERE id NOT IN "
            + "(SELECT id FROM publicaciones_vistas ORDER BY guardado_en DESC LIMIT " + LIMITE + ")")
    void limitarCantidad();
}
