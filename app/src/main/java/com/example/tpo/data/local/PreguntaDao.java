package com.example.tpo.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PreguntaDao {

    @Insert
    void agregar(PreguntaEntity entity);

    @Query("SELECT * FROM pregunta WHERE publicacionId = :publicacionId ORDER BY fecha ASC")
    List<PreguntaEntity> deLaPublicacion(String publicacionId);

    @Query("SELECT * FROM pregunta WHERE publicacionId = :publicacionId AND autorId = :autorId ORDER BY fecha ASC")
    List<PreguntaEntity> delUsuario(String publicacionId, String autorId);
}
