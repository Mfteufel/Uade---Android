package com.example.tpo.util;

import android.content.Context;
import android.text.format.DateUtils;

import com.example.tpo.R;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    /**
     * Fecha absoluta, "5 de septiembre de 2026". El renglón de zona muestra la
     * antigüedad relativa ("hace 5 h"), útil de un vistazo, pero el enunciado del
     * Detalle pide la fecha de publicación explícita.
     * <p>
     * {@link SimpleDateFormat} no es thread-safe: se instancia en cada llamada en
     * vez de guardarlo en un campo estático. Se usa pocas veces por pantalla.
     */
    public static String fechaCompleta(long fechaMillis) {
        return new SimpleDateFormat("d 'de' MMMM 'de' yyyy", LOCALE_AR).format(new Date(fechaMillis));
    }

    /** "septiembre de 2024", para el "miembro desde" del vendedor. */
    public static String mesYAnio(long fechaMillis) {
        return new SimpleDateFormat("MMMM 'de' yyyy", LOCALE_AR).format(new Date(fechaMillis));
    }

    /**
     * Reputación del vendedor lista para mostrar: "4,8 · 23 ventas", o el aviso de
     * que todavía no tiene calificaciones cuando no hizo ninguna venta.
     * <p>
     * Recibe primitivos y no un {@code Vendedor} para no acoplar esta clase de
     * utilidades al modelo. El puntaje se arma con {@link NumberFormat} (no
     * {@code String.valueOf}) porque en es-AR el separador decimal es la coma.
     */
    public static CharSequence reputacion(Context context, double reputacion, int cantidadVentas) {
        if (cantidadVentas <= 0) {
            return context.getString(R.string.vendedor_sin_calificaciones);
        }
        NumberFormat formato = NumberFormat.getNumberInstance(LOCALE_AR);
        formato.setMinimumFractionDigits(1);
        formato.setMaximumFractionDigits(1);
        String ventas = context.getResources().getQuantityString(
                R.plurals.vendedor_ventas, cantidadVentas, cantidadVentas);
        return context.getString(R.string.vendedor_reputacion_y_ventas, formato.format(reputacion), ventas);
    }
}
