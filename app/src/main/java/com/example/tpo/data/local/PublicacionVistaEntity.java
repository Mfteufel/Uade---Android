package com.example.tpo.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.tpo.model.Categoria;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Zona;

/**
 * Copia local de una publicacion que el
 * usuario tocó en el Home, para poder mostrarla de nuevo sin conexión.
 */
@Entity(tableName = "publicaciones_vistas")
public class PublicacionVistaEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String titulo;
    public String descripcion;
    public double precio;
    public String estado;
    public String categoria;
    public String zona;
    public long fechaPublicacion;
    public String nombreVendedor;

    @ColumnInfo(name = "guardado_en")
    public long guardadoEn;

    public static PublicacionVistaEntity desde(Publicacion publicacion, long guardadoEn) {
        PublicacionVistaEntity entidad = new PublicacionVistaEntity();
        entidad.id = publicacion.getId();
        entidad.titulo = publicacion.getTitulo();
        entidad.descripcion = publicacion.getDescripcion();
        entidad.precio = publicacion.getPrecio();
        entidad.estado = publicacion.getEstado().name();
        entidad.categoria = publicacion.getCategoria().name();
        entidad.zona = publicacion.getZona().name();
        entidad.fechaPublicacion = publicacion.getFechaPublicacion();
        entidad.nombreVendedor = publicacion.getNombreVendedor();
        entidad.guardadoEn = guardadoEn;
        return entidad;
    }

    public Publicacion aPublicacion() {
        return new Publicacion(
                id, titulo, descripcion, precio,
                EstadoArticulo.valueOf(estado),
                Categoria.valueOf(categoria),
                Zona.valueOf(zona),
                fechaPublicacion, nombreVendedor);
    }
}
