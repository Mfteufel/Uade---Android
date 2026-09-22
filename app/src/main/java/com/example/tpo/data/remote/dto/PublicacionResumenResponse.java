package com.example.tpo.data.remote.dto;

import androidx.annotation.Nullable;

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

    /**
     * El resumen del listado nunca trae dirección (no existe ese campo en
     * {@code PublicacionResumen} del backend); {@link PublicacionDetalleResponse}
     * la sobreescribe con el valor real que sí puede venir en el detalle.
     */
    @Nullable
    protected String direccionEntrega() {
        return null;
    }

    /**
     * {@code null} si algún campo obligatorio o algún valor de enum no coincide
     * con lo que la app conoce (por ejemplo, el backend agrega una categoría
     * nueva antes de que la app se actualice): así una publicación rara no
     * tira la página entera abajo, solo se descarta esa tarjeta.
     */
    @Nullable
    public Publicacion aModelo() {
        Categoria categoriaModelo = enumDe(Categoria.class, categoria);
        EstadoArticulo estadoModelo = enumDe(EstadoArticulo.class, estadoArticulo);
        Zona zonaModelo = enumDe(Zona.class, zona);
        EstadoPublicacion estadoPublicacionModelo = enumDe(EstadoPublicacion.class, estadoPublicacion);
        if (id == null || categoriaModelo == null || estadoModelo == null
                || zonaModelo == null || estadoPublicacionModelo == null) {
            return null;
        }
        Vendedor vendedor = new Vendedor(vendedorId, nombreVendedor, 0.0, 0, 0L);
        Publicacion publicacion = new Publicacion(id, titulo, descripcion, precio,
                estadoModelo, categoriaModelo, zonaModelo, fechaPublicacion, vendedor,
                cantidadFotos(), direccionEntrega());
        publicacion.setEstadoPublicacion(estadoPublicacionModelo);
        return publicacion;
    }

    @Nullable
    private static <E extends Enum<E>> E enumDe(Class<E> tipo, @Nullable String nombre) {
        if (nombre == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, nombre);
        } catch (IllegalArgumentException excepcion) {
            return null;
        }
    }
}
