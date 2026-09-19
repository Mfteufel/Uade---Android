package com.example.tpo.model;

import androidx.annotation.StringRes;

import com.example.tpo.R;

/**
 * Estado de la publicación en sí: si sigue en pie de venta, si el vendedor la
 * pausó o si ya se vendió. Es el mismo estado que se ve y se cambia tanto
 * desde el Detalle (Punto 4, {@code GestionPublicacionBottomSheet}) como
 * desde "Mis publicaciones" (Punto 5): una publicación {@code VENDIDA} no
 * tiene acciones disponibles; {@code ACTIVA} se puede pausar y
 * {@code PAUSADA} se puede reactivar.
 * <p>
 * <b>Ojo, no confundir con {@link EstadoArticulo}</b>: ese enum describe la
 * conservación del artículo (nuevo, como nuevo, usado); este describe el
 * <em>ciclo de vida de la publicación</em>. Los nombres se parecen a
 * propósito porque hablan del mismo objeto, pero son dos cosas independientes.
 */
public enum EstadoPublicacion {

    ACTIVA(R.string.estado_publicacion_activa),
    PAUSADA(R.string.estado_publicacion_pausada),
    VENDIDA(R.string.estado_publicacion_vendida);

    @StringRes
    private final int etiqueta;

    EstadoPublicacion(@StringRes int etiqueta) {
        this.etiqueta = etiqueta;
    }

    @StringRes
    public int getEtiqueta() {
        return etiqueta;
    }
}
