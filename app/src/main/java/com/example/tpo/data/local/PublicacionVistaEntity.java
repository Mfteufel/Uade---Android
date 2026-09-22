package com.example.tpo.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import com.example.tpo.model.Categoria;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Vendedor;
import com.example.tpo.model.Zona;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Punto 6 (modo sin conexión): copia local de una {@link Publicacion} que el
 * usuario consultó, para poder mostrarla de nuevo sin conexión.
 */
@Entity(tableName = "publicaciones_vistas", primaryKeys = {"usuarioId", "id"})
public class PublicacionVistaEntity {

    @NonNull
    public String usuarioId = "";
    @NonNull
    public String id = "";

    public String titulo;
    public String descripcion;
    public double precio;
    public String estado;
    public String categoria;
    public String zona;
    public long fechaPublicacion;

    public String vendedorId;
    public String vendedorNombre;
    public double vendedorReputacion;
    public int vendedorCantidadVentas;
    public long vendedorMiembroDesde;

    public int cantidadFotos;
    public String fotoPrincipalUrl;
    public String fotos;
    public String estadoPublicacion;
    public String direccionEntrega;

    /** Cuándo se cacheó (no confundir con {@link #fechaPublicacion}). Ordena el LRU. */
    public long guardadoEn;

    public static PublicacionVistaEntity desde(String usuarioId, Publicacion publicacion, long guardadoEn) {
        PublicacionVistaEntity entidad = new PublicacionVistaEntity();
        entidad.usuarioId = usuarioId;
        entidad.id = publicacion.getId();
        entidad.titulo = publicacion.getTitulo();
        entidad.descripcion = publicacion.getDescripcion();
        entidad.precio = publicacion.getPrecio();
        entidad.estado = publicacion.getEstado().name();
        entidad.categoria = publicacion.getCategoria().name();
        entidad.zona = publicacion.getZona().name();
        entidad.fechaPublicacion = publicacion.getFechaPublicacion();

        Vendedor vendedor = publicacion.getVendedor();
        entidad.vendedorId = vendedor.getId();
        entidad.vendedorNombre = vendedor.getNombre();
        entidad.vendedorReputacion = vendedor.getReputacion();
        entidad.vendedorCantidadVentas = vendedor.getCantidadVentas();
        entidad.vendedorMiembroDesde = vendedor.getMiembroDesde();

        entidad.cantidadFotos = publicacion.getCantidadFotos();
        entidad.fotoPrincipalUrl = publicacion.getFotoPrincipalUrl();
        entidad.fotos = String.join("|", publicacion.getFotos());
        entidad.estadoPublicacion = publicacion.getEstadoPublicacion().name();
        entidad.direccionEntrega = publicacion.getDireccionEntrega();
        entidad.guardadoEn = guardadoEn;
        return entidad;
    }

    public Publicacion aPublicacion() {
        Vendedor vendedor = new Vendedor(vendedorId, vendedorNombre, vendedorReputacion,
                vendedorCantidadVentas, vendedorMiembroDesde);
        Publicacion publicacion = new Publicacion(
                id, titulo, descripcion, precio,
                EstadoArticulo.valueOf(estado),
                Categoria.valueOf(categoria),
                Zona.valueOf(zona),
                fechaPublicacion, vendedor, cantidadFotos, direccionEntrega);
        publicacion.setEstadoPublicacion(EstadoPublicacion.valueOf(estadoPublicacion));
        publicacion.setFotoPrincipalUrl(fotoPrincipalUrl);
        publicacion.setFotos(fotosComoLista());
        return publicacion;
    }

    private List<String> fotosComoLista() {
        if (fotos == null || fotos.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(fotos.split("\\|")));
    }
}
