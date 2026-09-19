package com.example.tpo.data.local;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface OfertaDao {

    /** REPLACE sobre la PK compuesta: una oferta nueva reemplaza la vigente, no se apila. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(OfertaEntity entity);

    @Query("SELECT * FROM oferta WHERE publicacionId = :publicacionId")
    List<OfertaEntity> deLaPublicacion(String publicacionId);

    @Nullable
    @Query("SELECT * FROM oferta WHERE publicacionId = :publicacionId AND autorId = :autorId LIMIT 1")
    OfertaEntity delUsuario(String publicacionId, String autorId);

    /** Marca como aceptada la oferta de este usuario — Punto 8, acción del vendedor. */
    @Query("UPDATE oferta SET aceptada = 1 WHERE publicacionId = :publicacionId AND autorId = :autorId")
    void aceptar(String publicacionId, String autorId);
}
