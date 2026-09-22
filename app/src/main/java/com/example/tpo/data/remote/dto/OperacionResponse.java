package com.example.tpo.data.remote.dto;

import com.example.tpo.model.EstadoOperacion;
import com.example.tpo.model.Operacion;
import com.example.tpo.model.TipoOperacion;
import com.google.gson.annotations.SerializedName;

/**
 * JSON de una operación <em>vista por el usuario del token</em> (de ahí
 * {@code tipo}, {@code miCalificacion} y {@code puedeCalificar}), tal como la
 * devuelve {@code GET /operaciones}.
 * <p>
 * Del lado del servidor una operación es una oferta aceptada: el {@code id} es
 * el de la oferta. No trae fecha de aceptación, por eso el modelo recibe
 * {@code null} en {@code fechaOperacion}.
 */
public class OperacionResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("publicacionId")
    public String publicacionId;

    @SerializedName("articulo")
    public String articulo;

    @SerializedName("montoFinal")
    public double montoFinal;

    /** Epoch ms de la entrega confirmada por el comprador, o null. */
    @SerializedName("fechaEntrega")
    public Long fechaEntrega;

    /** "PENDIENTE_ENTREGA" o "ENTREGADA". */
    @SerializedName("estado")
    public String estado;

    /** "COMPRA" o "VENTA", desde el usuario del token. */
    @SerializedName("tipo")
    public String tipo;

    @SerializedName("comprador")
    public UsuarioResumenResponse comprador;

    @SerializedName("vendedor")
    public UsuarioResumenResponse vendedor;

    @SerializedName("miCalificacion")
    public CalificacionResponse miCalificacion;

    @SerializedName("puedeCalificar")
    public boolean puedeCalificar;

    /** Epoch ms: entrega + 7 días, o null si todavía no hubo entrega. */
    @SerializedName("calificableHasta")
    public Long calificableHasta;

    public Operacion aModelo() {
        return new Operacion(id, publicacionId, articulo, montoFinal, null,
                fechaEntrega, EstadoOperacion.valueOf(estado), comprador.aModelo(),
                vendedor.aModelo(), TipoOperacion.valueOf(tipo),
                miCalificacion == null ? null : miCalificacion.aModelo(),
                puedeCalificar, calificableHasta);
    }
}
