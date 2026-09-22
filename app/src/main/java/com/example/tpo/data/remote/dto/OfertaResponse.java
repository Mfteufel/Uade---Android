package com.example.tpo.data.remote.dto;

import androidx.annotation.Nullable;

/**
 * Forma en la que la API_Rest del TPO devuelve una oferta (Punto 7), tal cual
 * el contrato acordado con el backend de ofertas (versión 0.4.0).
 */
public class OfertaResponse {

    private String id;
    private String publicacionId;
    private String tituloPublicacion;
    @Nullable
    private String fotoPrincipalUrl;
    private String compradorId;
    private String nombreComprador;
    private String vendedorId;
    private String nombreVendedor;
    private double precio;
    @Nullable
    private String mensaje;
    private String estado;
    private String turno;
    private long fechaCreacion;
    private long venceEn;
    @Nullable
    private String direccionEntrega;

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

    public String getEstado() {
        return estado;
    }

    public String getTurno() {
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
}
