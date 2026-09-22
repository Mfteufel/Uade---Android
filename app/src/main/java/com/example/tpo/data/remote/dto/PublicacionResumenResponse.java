package com.example.tpo.data.remote.dto;

import com.example.tpo.model.Categoria;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Vendedor;
import com.example.tpo.model.Zona;
import com.google.gson.annotations.SerializedName;

public class PublicacionResumenResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("titulo")
    public String titulo;

    @SerializedName("descripcion")
    public String descripcion;

    @SerializedName("precio")
    public double precio;

    @SerializedName("categoria")
    public String categoria;

    @SerializedName("estadoArticulo")
    public String estadoArticulo;

    @SerializedName("zona")
    public String zona;

    @SerializedName("estadoPublicacion")
    public String estadoPublicacion;

    @SerializedName("fechaPublicacion")
    public long fechaPublicacion;

    @SerializedName("vendedorId")
    public String vendedorId;

    @SerializedName("nombreVendedor")
    public String nombreVendedor;

    @SerializedName("fotoPrincipalUrl")
    public String fotoPrincipalUrl;

    /** Sin galería propia: se aproxima con 1 si hay foto principal, 0 si no. */
    protected int cantidadFotos() {
        return fotoPrincipalUrl != null ? 1 : 0;
    }

    public Publicacion aModelo() {
        Vendedor vendedor = new Vendedor(vendedorId, nombreVendedor, 0.0, 0, 0L);
        Publicacion publicacion = new Publicacion(id, titulo, descripcion, precio,
                EstadoArticulo.valueOf(estadoArticulo), Categoria.valueOf(categoria), Zona.valueOf(zona),
                fechaPublicacion, vendedor, cantidadFotos(), null);
        publicacion.setEstadoPublicacion(EstadoPublicacion.valueOf(estadoPublicacion));
        return publicacion;
    }
}
