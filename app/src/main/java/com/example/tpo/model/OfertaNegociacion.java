package com.example.tpo.model;

import androidx.annotation.Nullable;

/**
 * Una oferta dentro de la negociación completa de "Mis ofertas" (Punto 7),
 * con la forma exacta del contrato del backend de ofertas (versión 0.4.0).
 */
public class OfertaNegociacion {

    private final String id;
    private final String publicacionId;
    private final String tituloPublicacion;
    @Nullable
    private final String fotoPrincipalUrl;
    private final String compradorId;
    private final String nombreComprador;
    private final String vendedorId;
    private final String nombreVendedor;
    private final double precio;
    @Nullable
    private final String mensaje;
    private final EstadoOferta estado;
    private final TurnoOferta turno;
    private final long fechaCreacion;
    private final long venceEn;
    @Nullable
    private final String direccionEntrega;

    public OfertaNegociacion(String id, String publicacionId, String tituloPublicacion,
                             @Nullable String fotoPrincipalUrl, String compradorId, String nombreComprador,
                             String vendedorId, String nombreVendedor, double precio, @Nullable String mensaje,
                             EstadoOferta estado, TurnoOferta turno, long fechaCreacion, long venceEn,
                             @Nullable String direccionEntrega) {
        this.id = id;
        this.publicacionId = publicacionId;
        this.tituloPublicacion = tituloPublicacion;
        this.fotoPrincipalUrl = fotoPrincipalUrl;
        this.compradorId = compradorId;
        this.nombreComprador = nombreComprador;
        this.vendedorId = vendedorId;
        this.nombreVendedor = nombreVendedor;
        this.precio = precio;
        this.mensaje = mensaje;
        this.estado = estado;
        this.turno = turno;
        this.fechaCreacion = fechaCreacion;
        this.venceEn = venceEn;
        this.direccionEntrega = direccionEntrega;
    }

    public String getId() {
        return id;
    }

    public String getPublicacionId() {
        return publicacionId;
    }

    public String getTituloPublicacion() {
        return tituloPublicacion;
    }

    @Nullable
    public String getFotoPrincipalUrl() {
        return fotoPrincipalUrl;
    }

    public String getCompradorId() {
        return compradorId;
    }

    public String getNombreComprador() {
        return nombreComprador;
    }

    public String getVendedorId() {
        return vendedorId;
    }

    public String getNombreVendedor() {
        return nombreVendedor;
    }

    public double getPrecio() {
        return precio;
    }

    @Nullable
    public String getMensaje() {
        return mensaje;
    }

    public EstadoOferta getEstado() {
        return estado;
    }

    public TurnoOferta getTurno() {
        return turno;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public long getVenceEn() {
        return venceEn;
    }

    @Nullable
    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    /** El nombre de "la otra parte", según si el usuario logueado es el comprador o el vendedor. */
    public String nombreContraparte(String usuarioId) {
        return compradorId.equals(usuarioId) ? nombreVendedor : nombreComprador;
    }

    /** true si le toca responder al usuario logueado: es parte, la oferta sigue PENDIENTE y el turno es el suyo. */
    public boolean meTocaResponder(String usuarioId) {
        if (estado != EstadoOferta.PENDIENTE) {
            return false;
        }
        boolean soyComprador = compradorId.equals(usuarioId);
        boolean soyVendedor = vendedorId.equals(usuarioId);
        return (soyComprador && turno == TurnoOferta.COMPRADOR)
                || (soyVendedor && turno == TurnoOferta.VENDEDOR);
    }
}
