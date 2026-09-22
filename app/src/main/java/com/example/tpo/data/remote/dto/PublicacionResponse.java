package com.example.tpo.data.remote.dto;

import androidx.annotation.Nullable;

import com.example.tpo.model.Categoria;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Vendedor;
import com.example.tpo.model.Zona;
import com.google.gson.annotations.SerializedName;

/**
 * JSON de una publicación del listado {@code GET /publicaciones}. Los enums
 * llegan con el nombre de la constante ("COMO_NUEVO", "PALERMO").
 */
public class PublicacionResponse {

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

    /** Epoch en milisegundos. */
    @SerializedName("fechaPublicacion")
    public long fechaPublicacion;

    @SerializedName("vendedorId")
    public String vendedorId;

    @SerializedName("nombreVendedor")
    public String nombreVendedor;

    @SerializedName("fotoPrincipalUrl")
    public String fotoPrincipalUrl;

    /**
     * Pasa al modelo del Home, o devuelve null si algún enum no es uno que la app
     * conozca: la tarjeta del listado necesita estado y zona para pintarse, y es
     * mejor no mostrar esa publicación que inventarle un valor.
     * <p>
     * El listado no trae la reputación del vendedor, así que el {@link Vendedor}
     * va solo con id y nombre; el perfil público la muestra aparte.
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
        Publicacion publicacion = new Publicacion(id, titulo, descripcion, precio,
                estadoModelo, categoriaModelo, zonaModelo, fechaPublicacion,
                new Vendedor(vendedorId, nombreVendedor, 0, 0, 0),
                fotoPrincipalUrl == null ? 0 : 1,
                null);
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
