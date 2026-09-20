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

    /** Id propio de la oferta (no la PK compuesta) — lo usan las pantallas de negociación (Punto 7). */
    @Nullable
    @Query("SELECT * FROM oferta WHERE id = :ofertaId LIMIT 1")
    OfertaEntity porId(String ofertaId);

    /** Usado por aceptar/rechazar: solo cambia el estado, no toca monto ni propuestoPor. */
    @Query("UPDATE oferta SET estado = :estado WHERE id = :ofertaId")
    void actualizarEstado(String ofertaId, String estado);

    /** Contraoferta: actualiza el último monto propuesto y quién lo propuso; reabre la negociación. */
    @Query("UPDATE oferta SET monto = :monto, propuestoPor = :propuestoPor, mensaje = :mensaje, estado = :estado WHERE id = :ofertaId")
    void contraofertar(String ofertaId, double monto, String propuestoPor, @Nullable String mensaje, String estado);

    @Query("SELECT * FROM oferta WHERE autorId = :usuarioId ORDER BY fecha DESC")
    List<OfertaEntity> comoComprador(String usuarioId);

    @Query("SELECT * FROM oferta WHERE vendedorId = :usuarioId ORDER BY fecha DESC")
    List<OfertaEntity> comoVendedor(String usuarioId);

    /** Un solo UPDATE, sin traer filas a Java: pasa de pendiente a vencida por fecha de vencimiento. */
    @Query("UPDATE oferta SET estado = :vencida WHERE estado = :pendiente AND fechaVencimiento < :ahora")
    void marcarVencidas(String pendiente, String vencida, long ahora);
}
