package com.example.tpo.model;

import androidx.annotation.StringRes;

import com.example.tpo.R;

/**
 * Opciones del filtro "cercanía a la zona del usuario" (Punto 3).
 * <p>
 * No usamos GPS ni distancias reales: la cercanía se resuelve comparando la zona
 * de la publicación con la zona del usuario logueado (ver {@link Zona}).
 */
public enum Cercania {

    /** Sin filtro de zona: se muestran publicaciones de todo el país. */
    TODAS(R.string.cercania_todas),
    /** Solo publicaciones de la misma zona exacta que el usuario. */
    MI_ZONA(R.string.cercania_mi_zona),
    /** Publicaciones de zonas de la misma región (zonas vecinas). */
    ZONAS_CERCANAS(R.string.cercania_zonas_cercanas);

    @StringRes
    private final int etiqueta;

    Cercania(@StringRes int etiqueta) {
        this.etiqueta = etiqueta;
    }

    @StringRes
    public int getEtiqueta() {
        return etiqueta;
    }
}
