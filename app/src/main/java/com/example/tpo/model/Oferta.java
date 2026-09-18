package com.example.tpo.model;

import java.io.Serializable;

/**
 * Una oferta de precio que un interesado le hizo al vendedor sobre una
 * publicación — acción "ofertar" del Punto 4.
 * <p>
 * Misma idea que {@link Pregunta} pero con un monto en vez de texto libre. Se
 * guarda en memoria en {@link com.example.tpo.data.OfertasPublicacion}, que
 * además impone la regla de que cada usuario tiene una sola oferta vigente por
 * publicación (ver el javadoc de esa clase).
 */
public class Oferta implements Serializable {

    private final String publicacionId;
    private final String autorId;
    private final String autorNombre;
    private final double monto;
    /** Momento del envío, en milisegundos desde epoch. */
    private final long fecha;

    public Oferta(String publicacionId, String autorId, String autorNombre, double monto, long fecha) {
        this.publicacionId = publicacionId;
        this.autorId = autorId;
        this.autorNombre = autorNombre;
        this.monto = monto;
        this.fecha = fecha;
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

    public double getMonto() {
        return monto;
    }

    public long getFecha() {
        return fecha;
    }
}
