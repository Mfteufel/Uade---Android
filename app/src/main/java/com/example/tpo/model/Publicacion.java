package com.example.tpo.model;

import java.io.Serializable;

/**
 * Una publicación del listado del Home.
 * <p>
 * Modela solo lo que el Punto 3 necesita mostrar y filtrar (título, precio,
 * estado, zona, categoría, descripción y fecha). El Punto 4 (Detalle) va a
 * necesitar más campos —galería de fotos, reputación del vendedor, etc.—; se
 * agregan cuando se implemente esa pantalla para no adelantar trabajo.
 * <p>
 * Implementa {@link Serializable} para poder viajar en un Bundle como argumento
 * de navegación hacia el Detalle.
 */
public class Publicacion implements Serializable {

    private final String id;
    private final String titulo;
    private final String descripcion;
    /**
     * Precio en pesos. Se usa double por simplicidad del TPO; si el backend
     * devolviera centavos convendría un long para evitar errores de redondeo.
     */
    private final double precio;
    private final EstadoArticulo estado;
    private final Categoria categoria;
    private final Zona zona;
    /**
     * Fecha de publicación en milisegundos desde epoch.
     * <p>
     * Se usa long y no java.time.LocalDate porque el minSdk del proyecto es 24 y
     * java.time recién está disponible desde API 26 (haría falta desugaring).
     */
    private final long fechaPublicacion;
    private final String nombreVendedor;

    public Publicacion(String id,
                       String titulo,
                       String descripcion,
                       double precio,
                       EstadoArticulo estado,
                       Categoria categoria,
                       Zona zona,
                       long fechaPublicacion,
                       String nombreVendedor) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.precio = precio;
        this.estado = estado;
        this.categoria = categoria;
        this.zona = zona;
        this.fechaPublicacion = fechaPublicacion;
        this.nombreVendedor = nombreVendedor;
    }

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    public EstadoArticulo getEstado() {
        return estado;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Zona getZona() {
        return zona;
    }

    public long getFechaPublicacion() {
        return fechaPublicacion;
    }

    public String getNombreVendedor() {
        return nombreVendedor;
    }
}
