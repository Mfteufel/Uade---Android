package com.example.tpo.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;

/**
 * Fila persistida de una oferta — mapea 1 a 1 con {@link com.example.tpo.model.Oferta}.
 * <p>
 * Clave compuesta ({@code publicacionId}, {@code autorId}): la regla de negocio
 * de "una sola oferta vigente por usuario y publicación" (antes impuesta a mano
 * en {@code OfertasPublicacion.guardar()}) sale gratis acá, porque insertar con
 * {@code OnConflictStrategy.REPLACE} sobre esta PK reemplaza la fila anterior
 * en vez de agregar una nueva.
 * <p>
 * {@code estado} se persiste como el {@code name()} del enum
 * {@link com.example.tpo.model.EstadoOferta}, mismo criterio que
 * {@link PublicacionEstadoEntity#estado}.
 */
@Entity(tableName = "oferta", primaryKeys = {"publicacionId", "autorId"})
public class OfertaEntity {

    /**
     * Id propio de la oferta (UUID), independiente de la PK compuesta: la PK
     * identifica "la oferta vigente de este usuario en esta publicación", pero
     * las acciones de negociación (aceptar/rechazar/contraofertar — Punto 7) se
     * disparan desde una pantalla que ya resolvió una oferta puntual y necesita
     * un id estable para pedir la actualización sin volver a conocer el par
     * (publicacionId, autorId).
     */
    @NonNull
    public String id = "";

    @NonNull
    public String publicacionId = "";

    @NonNull
    public String autorId = "";

    @NonNull
    public String autorNombre = "";

    @NonNull
    public String vendedorId = "";

    public double monto;

    public long fecha;

    public long fechaVencimiento;

    @NonNull
    public String estado = "PENDIENTE";

    @NonNull
    public String propuestoPor = "";

    @Nullable
    public String mensaje;
}
