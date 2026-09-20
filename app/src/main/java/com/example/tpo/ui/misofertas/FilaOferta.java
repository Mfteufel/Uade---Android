package com.example.tpo.ui.misofertas;

import com.example.tpo.model.Oferta;
import com.example.tpo.model.Publicacion;

/**
 * Une una {@link Oferta} con la {@link Publicacion} a la que corresponde, para
 * poder mostrar el título de la publicación en cada fila de "Mis ofertas" sin
 * que el adapter tenga que ir al repositorio — mismo criterio de "una sola
 * fuente de verdad por pantalla" que {@code GuardadosFragment}, que arma su
 * lista en dos pasos (ids guardados, después objetos completos) antes de
 * pasársela al adapter.
 */
public class FilaOferta {

    private final Oferta oferta;
    private final Publicacion publicacion;

    public FilaOferta(Oferta oferta, Publicacion publicacion) {
        this.oferta = oferta;
        this.publicacion = publicacion;
    }

    public Oferta getOferta() {
        return oferta;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }
}
