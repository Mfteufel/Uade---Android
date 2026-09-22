package com.example.tpo.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.tpo.model.Categoria;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Vendedor;
import com.example.tpo.model.Zona;

/**
 * Copia persistida de una {@link Publicacion} creada por el usuario desde el wizard
 * de "Publicar artículo" (Punto 5).
 * <p>
 * {@link com.example.tpo.data.PublicacionRepositoryMock#catalogo} es un
 * {@code ArrayList} en memoria que se reconstruye desde cero
 * ({@code crearCatalogoDePrueba()}) en cada arranque del proceso: sin esta tabla, una
 * publicación recién creada aparecía en Home/Detalle solo hasta el próximo reinicio
 * de la app, y después quedaba solo en "Mis publicaciones" (tabla {@code mi_publicacion}).
 * Al arrancar, {@code PublicacionRepositoryMock} lee todas las filas de acá y las
 * suma al catálogo antes de aplicar los overrides de estado
 * ({@link PublicacionEstadoEntity}), igual que el resto de las 28 publicaciones de
 * prueba.
 * <p>
 * No lleva {@code usuarioId} como {@link PublicacionVistaEntity}: esa tabla es un
 * cache por usuario que <i>vio</i> la publicación (para el modo sin conexión); esta
 * es el catálogo real, sin scope por quién la mira.
 */
@Entity(tableName = "publicacion_creada")
public class PublicacionCreadaEntity {

    @PrimaryKey
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
    public String direccionEntrega;

    public static PublicacionCreadaEntity desde(Publicacion publicacion) {
        PublicacionCreadaEntity entidad = new PublicacionCreadaEntity();
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
        entidad.direccionEntrega = publicacion.getDireccionEntrega();
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
        return publicacion;
    }
}
