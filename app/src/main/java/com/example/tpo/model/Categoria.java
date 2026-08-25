package com.example.tpo.model;

import androidx.annotation.StringRes;

import com.example.tpo.R;

/**
 * Categorías de producto disponibles en Ronda.
 * <p>
 * En la app final estas categorías las administra el sistema interno de Ronda y
 * deberían llegar desde la API. Mientras no exista el backend las dejamos fijas
 * acá; cuando se conecte Retrofit, esto pasa a ser un modelo con {@code id} y
 * {@code nombre} devuelto por el servidor.
 */
public enum Categoria {

    TECNOLOGIA(R.string.categoria_tecnologia),
    HOGAR(R.string.categoria_hogar),
    INDUMENTARIA(R.string.categoria_indumentaria),
    DEPORTES(R.string.categoria_deportes),
    LIBROS(R.string.categoria_libros),
    INSTRUMENTOS(R.string.categoria_instrumentos),
    BEBES(R.string.categoria_bebes),
    OTROS(R.string.categoria_otros);

    @StringRes
    private final int etiqueta;

    Categoria(@StringRes int etiqueta) {
        this.etiqueta = etiqueta;
    }

    @StringRes
    public int getEtiqueta() {
        return etiqueta;
    }
}
