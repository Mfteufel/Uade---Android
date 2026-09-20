package com.example.tpo.model;

import androidx.annotation.StringRes;

import com.example.tpo.R;

/**
 * Tipo de una operación <em>visto desde el usuario logueado</em>: la misma
 * operación es una COMPRA para el comprador y una VENTA para el vendedor.
 * <p>
 * Por eso no es un dato fijo de la operación sino de la respuesta: el servidor lo
 * calcula comparando al que pregunta contra el comprador y el vendedor.
 */
public enum TipoOperacion {

    COMPRA(R.string.historial_compras),
    VENTA(R.string.historial_ventas);

    /** Etiqueta en plural, usada tanto en el chip del filtro como en el encabezado de sección. */
    @StringRes
    private final int etiqueta;

    TipoOperacion(@StringRes int etiqueta) {
        this.etiqueta = etiqueta;
    }

    @StringRes
    public int getEtiqueta() {
        return etiqueta;
    }
}
