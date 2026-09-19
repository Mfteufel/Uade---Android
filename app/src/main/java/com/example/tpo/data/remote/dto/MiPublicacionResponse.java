package com.example.tpo.data.remote.dto;

import androidx.annotation.Nullable;

/**
 * Forma en la que la API_Rest del TPO devuelve cada publicación del vendedor
 * en el listado de "Mis publicaciones" (Punto 5).
 * <p>
 * Se separa del modelo de dominio {@link com.example.tpo.model.MiPublicacion}
 * a propósito: este DTO representa el contrato con el servidor (nombres de
 * campo en snake_case, estados como String) y puede cambiar sin que el resto
 * de la app se entere, siempre que el mapeo en
 * {@code MisPublicacionesRepositoryApi} se actualice.
 */
public class MiPublicacionResponse {

    private String id;
    private String titulo;
    private double precio;
    @Nullable
    private String fotoPrincipalUrl;
    private String estadoArticulo;
    private String estadoPublicacion;
    private long fechaPublicacion;

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

    public String getEstadoArticulo() {
        return estadoArticulo;
    }

    public String getEstadoPublicacion() {
        return estadoPublicacion;
    }

    public long getFechaPublicacion() {
        return fechaPublicacion;
    }
}
