package com.example.tpo.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import com.example.tpo.model.Categoria;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Vendedor;
import com.example.tpo.model.Zona;

/**
 * Punto 6 (modo sin conexión): copia local de una {@link Publicacion} que el
 * usuario tocó en el Home, para poder mostrarla de nuevo sin conexión.
 * <p>
 * Clave compuesta ({@code usuarioId}, {@code id}), mismo criterio que
 * {@link PublicacionGuardadaEntity}: evita que dos usuarios que prueban la app
 * en el mismo dispositivo mezclen su historial, y volver a ver la misma
 * publicación simplemente reemplaza la fila en vez de duplicarla.
 * <p>
 * A diferencia de {@link PublicacionGuardadaEntity} (que solo guarda el id
 * porque siempre puede pedirle los datos de nuevo a {@code PublicacionRepositoryMock}),
 * acá se guarda una copia completa: sin conexión no hay a quién pedírsela.
 * Los enums se guardan como su {@code name()}.
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
    public String estadoPublicacion;

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
        entidad.estadoPublicacion = publicacion.getEstadoPublicacion().name();
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
                fechaPublicacion, vendedor, cantidadFotos);
        publicacion.setEstadoPublicacion(EstadoPublicacion.valueOf(estadoPublicacion));
        return publicacion;
    }
}
