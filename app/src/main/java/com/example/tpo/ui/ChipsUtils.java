package com.example.tpo.ui;

import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.tpo.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/**
 * Helpers para armar grupos de chips a partir de los enums del modelo.
 * <p>
 * Los usan tanto el Home (categorías y ordenamiento) como el bottom sheet de
 * filtros (estado y cercanía), así que la lógica vive en un solo lugar.
 */
public final class ChipsUtils {

    private ChipsUtils() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Crea un chip con el estilo de Material y lo agrega al grupo.
     * <p>
     * El chip se infla desde view_chip_filtro.xml en vez de construirse con
     * {@code new Chip(context)}: creado a mano no toma el estilo del tema y se ve
     * como un TextView pelado.
     *
     * @param grupo  grupo donde se agrega el chip.
     * @param texto  etiqueta visible.
     * @param valor  valor del modelo que representa (se guarda en el tag del chip,
     *               así después se recupera sin necesidad de mapas auxiliares).
     * @param marcado si el chip nace seleccionado.
     */
    public static Chip agregarChip(ChipGroup grupo, CharSequence texto, Object valor, boolean marcado) {
        LayoutInflater inflater = LayoutInflater.from(grupo.getContext());
        Chip chip = (Chip) inflater.inflate(R.layout.view_chip_filtro, grupo, false);
        // Un id propio es obligatorio: el ChipGroup identifica al chip seleccionado
        // por id, y todos los chips inflados del mismo XML comparten el id original.
        chip.setId(View.generateViewId());

        // Y con el id generado viene un problema: generateViewId() devuelve números
        // distintos en cada instancia de la pantalla, pero Android guarda y restaura
        // el estado de las vistas justamente por id. Al recrearse el Fragment, el
        // "tildado" guardado puede caer sobre un chip que ahora es otro, y quedaría
        // marcada una categoría que el usuario nunca eligió.
        // Como el chip que va marcado sale siempre del filtro (que sí se guarda en
        // onSaveInstanceState), el estado que guarda el sistema sobra: se desactiva.
        chip.setSaveEnabled(false);

        chip.setText(texto);
        chip.setTag(valor);
        grupo.addView(chip);
        // Se marca después de agregarlo para que el ChipGroup ya lo conozca y
        // respete la selección única.
        chip.setChecked(marcado);
        return chip;
    }

    /**
     * Devuelve el valor asociado al chip seleccionado de un grupo de selección
     * única, o {@code null} si no hay ninguno marcado.
     * <p>
     * Quien llama tiene que castear al enum que corresponda.
     */
    @Nullable
    public static Object valorSeleccionado(ChipGroup grupo) {
        int idSeleccionado = grupo.getCheckedChipId();
        if (idSeleccionado == View.NO_ID) {
            return null;
        }
        View chip = grupo.findViewById(idSeleccionado);
        return chip == null ? null : chip.getTag();
    }
}
