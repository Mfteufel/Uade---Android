package com.example.tpo.model;

import androidx.annotation.StringRes;

import com.example.tpo.R;

/**
 * Estado de una publicación dentro del ciclo de vida de "Mis publicaciones" (Punto 5).
 * <p>
 * No confundir con {@link EstadoArticulo}: ese describe la conservación del
 * artículo (nuevo, usado, etc.), mientras que este describe si la publicación
 * está visible en el Home. Una publicación {@code VENDIDA} no tiene acciones
 * disponibles; {@code ACTIVA} se puede pausar y {@code PAUSADA} se puede reactivar.
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
