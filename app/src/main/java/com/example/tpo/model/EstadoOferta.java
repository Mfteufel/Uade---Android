package com.example.tpo.model;

import androidx.annotation.StringRes;

import com.example.tpo.R;

/**
 * Estado de una {@link Oferta} dentro de la negociación — Punto 7.
 * <p>
 * {@code PENDIENTE} es el único estado en el que la oferta admite acciones
 * (aceptar, rechazar, contraofertar); los otros tres son terminales dentro de
 * esa publicación para ese comprador — una nueva oferta del mismo usuario
 * reemplaza a la anterior (ver {@code OfertasPublicacion}), no reabre esta.
 */
public enum EstadoOferta {

    PENDIENTE(R.string.estado_oferta_pendiente),
    ACEPTADA(R.string.estado_oferta_aceptada),
    RECHAZADA(R.string.estado_oferta_rechazada),
    VENCIDA(R.string.estado_oferta_vencida);

    @StringRes
    private final int etiqueta;

    EstadoOferta(@StringRes int etiqueta) {
        this.etiqueta = etiqueta;
    }

    @StringRes
    public int getEtiqueta() {
        return etiqueta;
    }
}
