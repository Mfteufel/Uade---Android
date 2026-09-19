package com.example.tpo.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PublicacionGuardadaDao {

    @Query("SELECT EXISTS(SELECT 1 FROM publicacion_guardada "
            + "WHERE usuarioId = :usuarioId AND publicacionId = :publicacionId)")
    boolean estaGuardada(String usuarioId, String publicacionId);

    /** Ids guardados por el usuario, el más reciente primero — pantalla "Guardados". */
    @Query("SELECT publicacionId FROM publicacion_guardada WHERE usuarioId = :usuarioId ORDER BY fechaGuardado DESC")
    List<String> listarIds(String usuarioId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(PublicacionGuardadaEntity entity);

    @Query("DELETE FROM publicacion_guardada WHERE usuarioId = :usuarioId AND publicacionId = :publicacionId")
    void quitar(String usuarioId, String publicacionId);
}
