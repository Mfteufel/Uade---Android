package com.example.tpo.model;

/**
 * Etapa de una operación entre comprador y vendedor.
 * <ul>
 *     <li>{@link #PENDIENTE_ENTREGA}: el vendedor aceptó la oferta (Punto 7) pero
 *     todavía no se hizo la entrega en mano (Punto 8).</li>
 *     <li>{@link #ENTREGADA}: la entrega se confirmó. Recién ahí la operación
 *     cuenta como <em>concretada</em>: aparece en el historial, suma a la
 *     reputación y abre la ventana de 7 días para calificar.</li>
 * </ul>
 */
public enum EstadoOperacion {
    PENDIENTE_ENTREGA,
    ENTREGADA
}
