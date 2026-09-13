package com.example.tpo.ui;

import android.widget.TextView;

import com.example.tpo.R;
import com.example.tpo.model.Vendedor;
import com.example.tpo.util.FormatoUtils;

/**
 * Helpers de UI del vendedor compartidos entre el Detalle y el perfil público.
 * <p>
 * Mismo espíritu que {@link ChipsUtils}: evita duplicar en dos Fragments las
 * mismas líneas de "pintá la reputación y sacá la estrella si no tiene ventas".
 */
public final class VendedorUi {

    private VendedorUi() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Escribe en {@code destino} la reputación del vendedor ("4,8 · 23 ventas" o
     * "Sin calificaciones todavía") y muestra la estrella como drawableStart solo
     * si el vendedor tiene al menos una venta: sin puntaje la estrella confunde.
     */
    public static void pintarReputacion(TextView destino, Vendedor vendedor) {
        destino.setText(FormatoUtils.reputacion(
                destino.getContext(), vendedor.getReputacion(), vendedor.getCantidadVentas()));

        int estrella = vendedor.getCantidadVentas() > 0 ? R.drawable.ic_estrella : 0;
        // minSdk 24: el método nativo de TextView (API 17+) alcanza, sin TextViewCompat.
        destino.setCompoundDrawablesRelativeWithIntrinsicBounds(estrella, 0, 0, 0);
    }
}
