package com.example.tpo.model;

/**
 * Quién tiene que responder en una {@link OfertaNegociacion} — Punto 7.
 * <p>
 * Sin sentido (pero el servidor lo manda igual, con el último valor que tuvo)
 * cuando la oferta ya no está {@code PENDIENTE}: ver {@code docs/ofertas-api.md}.
 */
public enum TurnoOferta {
    VENDEDOR,
    COMPRADOR
}
