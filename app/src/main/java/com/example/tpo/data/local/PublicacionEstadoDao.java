package com.example.tpo.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PublicacionEstadoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void fijarEstado(PublicacionEstadoEntity entity);

    /** Todos los overrides guardados, para aplicarlos sobre el catálogo mock al arrancar. */
    @Query("SELECT * FROM publicacion_estado")
    List<PublicacionEstadoEntity> obtenerTodos();
}
