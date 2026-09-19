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
    /**
     * true si el vendedor ya aceptó esta oferta. No es final ni se recibe por
     * constructor porque toda oferta arranca sin aceptar (mismo criterio que
     * {@link Publicacion#getEstadoPublicacion()}): se acepta después, desde
     * "Gestionar publicación". Punto 8: es lo que habilita al comprador a ver
     * la dirección de entrega.
     */
    private boolean aceptada = false;

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

    public boolean isAceptada() {
        return aceptada;
    }

    public void setAceptada(boolean aceptada) {
        this.aceptada = aceptada;
    }
}
