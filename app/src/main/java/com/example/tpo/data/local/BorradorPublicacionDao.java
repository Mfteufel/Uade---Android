package com.example.tpo.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import androidx.annotation.Nullable;

/**
 * Acceso a la fila (única) del borrador de "Publicar artículo".
 */
@Dao
public interface BorradorPublicacionDao {

    /** Inserta o reemplaza el borrador. Como el id es siempre el mismo, esto nunca acumula filas. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(BorradorPublicacionEntity borrador);

    @Query("SELECT * FROM borrador_publicacion WHERE id = " + BorradorPublicacionEntity.ID_UNICO + " LIMIT 1")
    @Nullable
    BorradorPublicacionEntity obtener();

    @Query("DELETE FROM borrador_publicacion")
    void borrar();
}
