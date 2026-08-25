package com.example.tpo.data;

import com.example.tpo.model.Publicacion;

import java.util.List;

/**
 * Resultado de una consulta paginada al repositorio.
 * <p>
 * Además de los elementos de la página trae el total de resultados y si quedan
 * más páginas, que es lo que el Home necesita para saber si tiene que seguir
 * pidiendo al hacer scroll.
 */
public class PaginaPublicaciones {

    private final List<Publicacion> publicaciones;
    /** Número de página devuelta, empezando en 0. */
    private final int pagina;
    /** true si existe al menos una página más después de ésta. */
    private final boolean hayMas;
    /** Total de publicaciones que matchean el filtro (no solo las de esta página). */
    private final int totalResultados;

    public PaginaPublicaciones(List<Publicacion> publicaciones,
                               int pagina,
                               boolean hayMas,
                               int totalResultados) {
        this.publicaciones = publicaciones;
        this.pagina = pagina;
        this.hayMas = hayMas;
        this.totalResultados = totalResultados;
    }

    public List<Publicacion> getPublicaciones() {
        return publicaciones;
    }

    public int getPagina() {
        return pagina;
    }

    public boolean hayMas() {
        return hayMas;
    }

    public int getTotalResultados() {
        return totalResultados;
    }
}
