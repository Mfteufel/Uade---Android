package com.example.tpo.model;

import androidx.annotation.StringRes;

import com.example.tpo.R;

/**
 * Criterios de ordenamiento del listado del Home (Punto 3 del enunciado).
 */
public enum OrdenPublicaciones {

    /** Más recientes primero (orden por defecto). */
    RECIENTES(R.string.orden_recientes),
    /** Menor precio primero. */
    PRECIO_MENOR(R.string.orden_precio_menor),
    /** Mayor precio primero. */
    PRECIO_MAYOR(R.string.orden_precio_mayor);

    @StringRes
    private final int etiqueta;

    OrdenPublicaciones(@StringRes int etiqueta) {
        this.etiqueta = etiqueta;
    }

    @StringRes
    public int getEtiqueta() {
        return etiqueta;
    }
}
