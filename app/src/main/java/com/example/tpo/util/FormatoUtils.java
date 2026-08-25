package com.example.tpo.util;

import android.content.Context;
import android.text.format.DateUtils;

import com.example.tpo.R;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formateo de precios y fechas para mostrar en pantalla.
 */
public final class FormatoUtils {

    /**
     * Locale fijo es-AR: el formato no depende del idioma del teléfono.
     * <p>
     * Se construye con forLanguageTag y no con {@code new Locale("es", "AR")}
     * porque ese constructor quedó deprecado.
     */
    private static final Locale LOCALE_AR = Locale.forLanguageTag("es-AR");

    private FormatoUtils() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Formatea un precio como "$ 45.000".
     * <p>
     * Se arma con NumberFormat (separador de miles según es-AR) en vez de
     * getCurrencyInstance() para tener control del símbolo y no arrastrar los
     * decimales, que en artículos usados no aportan nada.
     */
    public static String precio(double valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(LOCALE_AR);
        formato.setMaximumFractionDigits(0);
        return "$ " + formato.format(valor);
    }

    /**
     * Devuelve la antigüedad de la publicación en texto ("hace 3 días").
     * <p>
     * Se usa DateUtils de Android, que ya viene traducido y maneja los plurales;
     * escribir esto a mano sería reinventar la rueda. Para publicaciones de menos
     * de un minuto DateUtils devuelve "hace 0 minutos", así que ese caso se
     * resuelve aparte con un string propio.
     */
    public static CharSequence antiguedad(Context context, long fechaMillis) {
        long ahora = System.currentTimeMillis();
        if (ahora - fechaMillis < DateUtils.MINUTE_IN_MILLIS) {
            return context.getString(R.string.publicado_recien);
        }
        return DateUtils.getRelativeTimeSpanString(
                fechaMillis,
                ahora,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE);
    }
}
