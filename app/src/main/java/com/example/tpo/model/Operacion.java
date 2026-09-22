package com.example.tpo.model;

import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Una operación entre comprador y vendedor (Punto 9): nace cuando el vendedor
 * acepta una oferta (Punto 7) y se concreta cuando el comprador confirma que
 * recibió el artículo.
 * <p>
 * Modela exactamente lo que el servidor devuelve en {@code GET /operaciones}: los
 * datos de la operación más tres campos calculados <em>para el usuario que
 * pregunta</em> ({@link #tipo}, {@link #miCalificacion} y la ventana para
 * calificar). La app no calcula esa ventana: si lo hiciera con el reloj del
 * teléfono, cambiar la hora del dispositivo alcanzaría para calificar fuera de
 * plazo. El servidor decide y además vuelve a validar al recibir la calificación.
 * <p>
 * Guarda una copia del título del artículo y del monto final: el historial tiene
 * que mostrar lo que se acordó, aunque después la publicación se edite o se borre.
 */
public class Operacion implements Serializable {

    private final String id;

    /** Publicación de origen. Puede ser {@code null} en datos viejos o de prueba. */
    @Nullable
    private final String publicacionId;

    private final String tituloArticulo;

    /** Precio final acordado (el de la oferta o contraoferta aceptada), en pesos. */
    private final double montoFinal;

    /**
     * Cuándo se cerró el trato (oferta aceptada), en milisegundos desde epoch, o
     * {@code null} si no se conoce: el backend no guarda la fecha de aceptación,
     * porque la fecha que importa para el historial y la reputación es la de entrega.
     */
    @Nullable
    private final Long fechaOperacion;

    /** Cuándo se confirmó la entrega, o {@code null} si todavía no se entregó. */
    @Nullable
    private final Long fechaEntrega;

    private final EstadoOperacion estado;

    private final UsuarioResumen comprador;
    private final UsuarioResumen vendedor;

    /** COMPRA o VENTA desde el punto de vista del usuario logueado. */
    private final TipoOperacion tipo;

    /** Lo que el usuario logueado le dejó a la contraparte, o {@code null} si todavía no calificó. */
    @Nullable
    private final Calificacion miCalificacion;

    /** Lo decide el servidor: entregada, dentro de los 7 días y sin calificación previa. */
    private final boolean puedeCalificar;

    /** Fin de la ventana para calificar (entrega + 7 días), o {@code null} si no hubo entrega. */
    @Nullable
    private final Long calificableHasta;

    public Operacion(String id,
                     @Nullable String publicacionId,
                     String tituloArticulo,
                     double montoFinal,
                     @Nullable Long fechaOperacion,
                     @Nullable Long fechaEntrega,
                     EstadoOperacion estado,
                     UsuarioResumen comprador,
                     UsuarioResumen vendedor,
                     TipoOperacion tipo,
                     @Nullable Calificacion miCalificacion,
                     boolean puedeCalificar,
                     @Nullable Long calificableHasta) {
        this.id = id;
        this.publicacionId = publicacionId;
        this.tituloArticulo = tituloArticulo;
        this.montoFinal = montoFinal;
        this.fechaOperacion = fechaOperacion;
        this.fechaEntrega = fechaEntrega;
        this.estado = estado;
        this.comprador = comprador;
        this.vendedor = vendedor;
        this.tipo = tipo;
        this.miCalificacion = miCalificacion;
        this.puedeCalificar = puedeCalificar;
        this.calificableHasta = calificableHasta;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public String getPublicacionId() {
        return publicacionId;
    }

    public String getTituloArticulo() {
        return tituloArticulo;
    }

    public double getMontoFinal() {
        return montoFinal;
    }

    @Nullable
    public Long getFechaOperacion() {
        return fechaOperacion;
    }

    @Nullable
    public Long getFechaEntrega() {
        return fechaEntrega;
    }

    public EstadoOperacion getEstado() {
        return estado;
    }

    public UsuarioResumen getComprador() {
        return comprador;
    }

    public UsuarioResumen getVendedor() {
        return vendedor;
    }

    public TipoOperacion getTipo() {
        return tipo;
    }

    /** La otra parte: el vendedor si yo compré, el comprador si yo vendí. */
    public UsuarioResumen getContraparte() {
        return tipo == TipoOperacion.COMPRA ? vendedor : comprador;
    }

    @Nullable
    public Calificacion getMiCalificacion() {
        return miCalificacion;
    }

    public boolean yaCalifique() {
        return miCalificacion != null;
    }

    public boolean puedeCalificar() {
        return puedeCalificar;
    }

    public boolean estaPendienteDeEntrega() {
        return estado == EstadoOperacion.PENDIENTE_ENTREGA;
    }

    /**
     * Solo el comprador confirma la entrega: es quien sabe que recibió el artículo.
     * El servidor vuelve a validarlo; esto es para mostrar u ocultar el botón.
     */
    public boolean puedeConfirmarEntrega() {
        return tipo == TipoOperacion.COMPRA && estaPendienteDeEntrega();
    }

    @Nullable
    public Long getCalificableHasta() {
        return calificableHasta;
    }

    /**
     * Fecha con la que se muestra y se filtra la operación en el historial: la de
     * entrega, que es cuando se concretó. Si todavía no se entregó, la del acuerdo,
     * que puede no conocerse ({@code null}).
     */
    @Nullable
    public Long getFechaReferencia() {
        return fechaEntrega != null ? fechaEntrega : fechaOperacion;
    }
}
