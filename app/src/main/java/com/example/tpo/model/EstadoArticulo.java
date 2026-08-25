package com.example.tpo.model;

import androidx.annotation.StringRes;

import com.example.tpo.R;

/**
 * Estado de conservación de un artículo publicado.
 * <p>
 * El enunciado (Punto 3) define exactamente estos tres valores, por eso se modela
 * como enum y no como String: evita valores inválidos y nos deja usarlo directo
 * como filtro sin validar nada a mano.
 */
public enum EstadoArticulo {

    NUEVO(R.string.estado_nuevo),
    COMO_NUEVO(R.string.estado_como_nuevo),
    USADO(R.string.estado_usado);

    /** Texto visible al usuario. Vive en strings.xml, no hardcodeado en el enum. */
    @StringRes
    private final int etiqueta;

    EstadoArticulo(@StringRes int etiqueta) {
        this.etiqueta = etiqueta;
    }

    @StringRes
    public int getEtiqueta() {
        return etiqueta;
    }
}
