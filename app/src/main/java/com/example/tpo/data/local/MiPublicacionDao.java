package com.example.tpo.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MiPublicacionDao {

    @Insert
    void insertar(PublicacionMiaEntity entity);

    @Query("SELECT * FROM mi_publicacion WHERE vendedorId = :vendedorId ORDER BY fechaPublicacion DESC")
    List<PublicacionMiaEntity> listarPorVendedor(String vendedorId);

    @Query("UPDATE mi_publicacion SET estadoPublicacion = :estadoPublicacion WHERE id = :id")
    void actualizarEstado(String id, String estadoPublicacion);
}
