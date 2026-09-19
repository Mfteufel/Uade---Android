package com.example.tpo.data.remote.dto;

import com.example.tpo.model.EstadoOperacion;
import com.example.tpo.model.Operacion;
import com.example.tpo.model.TipoOperacion;
import com.google.gson.annotations.SerializedName;

/**
 * JSON de una operación <em>vista por el usuario del token</em> (de ahí
 * {@code tipo}, {@code mi_calificacion} y {@code puede_calificar}). Ver
 * {@code docs/contrato-api-perfil-historial.md}.
 */
public class OperacionResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("publicacion_id")
    public String publicacionId;

    @SerializedName("articulo")
    public String articulo;

    @SerializedName("monto_final")
    public double montoFinal;

    /** Epoch ms del acuerdo (oferta aceptada). */
    @SerializedName("fecha_operacion")
    public long fechaOperacion;

    /** Epoch ms de la entrega, o null. */
    @SerializedName("fecha_entrega")
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

    @SerializedName("mi_calificacion")
    public CalificacionResponse miCalificacion;

    @SerializedName("puede_calificar")
    public boolean puedeCalificar;

    /** Epoch ms: entrega + 7 días, o null si no hubo entrega. */
    @SerializedName("calificable_hasta")
    public Long calificableHasta;

    public Operacion aModelo() {
        return new Operacion(id, publicacionId, articulo, montoFinal, fechaOperacion,
                fechaEntrega, EstadoOperacion.valueOf(estado), comprador.aModelo(),
                vendedor.aModelo(), TipoOperacion.valueOf(tipo),
                miCalificacion == null ? null : miCalificacion.aModelo(),
                puedeCalificar, calificableHasta);
    }
}
