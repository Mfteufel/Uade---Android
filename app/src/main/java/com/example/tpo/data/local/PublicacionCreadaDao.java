package com.example.tpo.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PublicacionCreadaDao {

    @Insert
    void insertar(PublicacionCreadaEntity entity);

    /** Todas las publicaciones creadas por el usuario, para sumarlas al catálogo mock al arrancar. */
    @Query("SELECT * FROM publicacion_creada ORDER BY fechaPublicacion DESC")
    List<PublicacionCreadaEntity> listarTodas();
}
