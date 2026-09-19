package com.example.tpo.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * Fila persistida de una oferta — mapea 1 a 1 con {@link com.example.tpo.model.Oferta}.
 * <p>
 * Clave compuesta ({@code publicacionId}, {@code autorId}): la regla de negocio
 * de "una sola oferta vigente por usuario y publicación" (antes impuesta a mano
 * en {@code OfertasPublicacion.guardar()}) sale gratis acá, porque insertar con
 * {@code OnConflictStrategy.REPLACE} sobre esta PK reemplaza la fila anterior
 * en vez de agregar una nueva.
 */
@Entity(tableName = "oferta", primaryKeys = {"publicacionId", "autorId"})
public class OfertaEntity {

    @NonNull
    public String publicacionId = "";

    @NonNull
    public String autorId = "";

    @NonNull
    public String autorNombre = "";

    public double monto;

    public long fecha;

    /**
     * true si el vendedor ya aceptó esta oferta — Punto 8: hasta que esto no
     * pase, el comprador no puede ver la dirección de entrega. El resto del
     * ciclo de la oferta (rechazar, contraofertar, vencimiento) es del Punto 7
     * y todavía no existe, así que por ahora esto es lo único que hace falta.
     */
    public boolean aceptada;
}
