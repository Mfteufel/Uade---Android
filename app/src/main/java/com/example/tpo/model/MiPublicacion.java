package com.example.tpo.model;

import androidx.annotation.Nullable;

/**
 * Ítem del listado "Mis publicaciones" (Punto 5).
 * <p>
 * No reutiliza {@link Publicacion} porque esa clase modela lo que el Home
 * necesita mostrar de publicaciones ajenas (zona, vendedor, etc.) y esta
 * pantalla necesita en cambio el {@link EstadoPublicacion}, que no tiene
 * sentido para el Home. Separarlas evita forzar campos irrelevantes en uno de
 * los dos casos de uso.
 */
public class MiPublicacion {

    private final String id;
    private final String titulo;
    private final double precio;
    @Nullable
    private final String fotoPrincipalUrl;
    private final EstadoArticulo estadoArticulo;
    private final EstadoPublicacion estadoPublicacion;
    private final long fechaPublicacion;

    public MiPublicacion(String id,
                         String titulo,
                         double precio,
                         @Nullable String fotoPrincipalUrl,
                         EstadoArticulo estadoArticulo,
                         EstadoPublicacion estadoPublicacion,
                         long fechaPublicacion) {
        this.id = id;
        this.titulo = titulo;
        this.precio = precio;
        this.fotoPrincipalUrl = fotoPrincipalUrl;
        this.estadoArticulo = estadoArticulo;
        this.estadoPublicacion = estadoPublicacion;
        this.fechaPublicacion = fechaPublicacion;
    }

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public double getPrecio() {
        return precio;
    }

    @Nullable
    public String getFotoPrincipalUrl() {
        return fotoPrincipalUrl;
    }

    public EstadoArticulo getEstadoArticulo() {
        return estadoArticulo;
    }

    public EstadoPublicacion getEstadoPublicacion() {
        return estadoPublicacion;
    }

    public long getFechaPublicacion() {
        return fechaPublicacion;
    }
}
