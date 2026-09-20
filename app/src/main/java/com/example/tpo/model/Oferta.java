package com.example.tpo.model;

import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Una oferta de precio que un interesado le hizo al vendedor sobre una
 * publicación — acción "ofertar" del Punto 4, extendida con negociación
 * completa en el Punto 7.
 * <p>
 * Misma idea que {@link Pregunta} pero con un monto en vez de texto libre. Se
 * guarda en memoria en {@link com.example.tpo.data.OfertasPublicacion}, que
 * además impone la regla de que cada usuario tiene una sola oferta vigente por
 * publicación (ver el javadoc de esa clase).
 * <p>
 * {@code monto} representa el último precio propuesto, no necesariamente el
 * original: cada contraoferta lo actualiza en la misma fila (ver
 * {@code propuestoPor}) en vez de agregar una oferta nueva.
 */
public class Oferta implements Serializable {

    private final String id;
    private final String publicacionId;
    private final String autorId;
    private final String autorNombre;
    private final String vendedorId;
    private final double monto;
    /** Momento del envío original, en milisegundos desde epoch. */
    private final long fecha;
    /** Momento en que la oferta deja de estar vigente si nadie respondió, en milisegundos desde epoch. */
    private final long fechaVencimiento;
    private final EstadoOferta estado;
    /** Id (autorId o vendedorId) de quien hizo la última propuesta vigente. */
    private final String propuestoPor;
    @Nullable
    private final String mensaje;

    public Oferta(String id, String publicacionId, String autorId, String autorNombre,
                  String vendedorId, double monto, long fecha, long fechaVencimiento,
                  EstadoOferta estado, String propuestoPor, @Nullable String mensaje) {
        this.id = id;
        this.publicacionId = publicacionId;
        this.autorId = autorId;
        this.autorNombre = autorNombre;
        this.vendedorId = vendedorId;
        this.monto = monto;
        this.fecha = fecha;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = estado;
        this.propuestoPor = propuestoPor;
        this.mensaje = mensaje;
    }

    public String getId() {
        return id;
    }

    public String getPublicacionId() {
        return publicacionId;
    }

    public String getAutorId() {
        return autorId;
    }

    public String getAutorNombre() {
        return autorNombre;
    }

    public String getVendedorId() {
        return vendedorId;
    }

    public double getMonto() {
        return monto;
    }

    public long getFecha() {
        return fecha;
    }

    public long getFechaVencimiento() {
        return fechaVencimiento;
    }

    public EstadoOferta getEstado() {
        return estado;
    }

    public String getPropuestoPor() {
        return propuestoPor;
    }

    @Nullable
    public String getMensaje() {
        return mensaje;
    }
}
