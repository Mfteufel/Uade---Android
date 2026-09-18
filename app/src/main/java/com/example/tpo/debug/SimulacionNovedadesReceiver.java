package com.example.tpo.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.example.tpo.data.BusquedaGuardadaRepositoryMock;
import com.example.tpo.data.FavoritoRepositoryMock;
import com.example.tpo.model.Categoria;

import java.util.Locale;

/**
 * Simula vía ADB las novedades que en la app final empujaría el backend
 * (Punto 10) — como todavía no hay backend, es la única forma de
 * generarlas mientras la app está corriendo:
 * <pre>
 * adb shell am broadcast -a com.example.tpo.debug.SIMULAR_BAJA_PRECIO
 * adb shell am broadcast -a com.example.tpo.debug.SIMULAR_PUBLICACION_NUEVA \
 *     --es titulo "iPhone 13 reacondicionado" --es categoria TECNOLOGIA
 * </pre>
 */
public class SimulacionNovedadesReceiver extends BroadcastReceiver {

    public static final String ACCION_BAJA_PRECIO = "com.example.tpo.debug.SIMULAR_BAJA_PRECIO";
    public static final String ACCION_PUBLICACION_NUEVA = "com.example.tpo.debug.SIMULAR_PUBLICACION_NUEVA";
    public static final String EXTRA_TITULO = "titulo";
    public static final String EXTRA_CATEGORIA = "categoria";

    /** Avisa a quien registró el receiver para que refresque badges en pantalla. */
    public interface Listener {
        void onNovedadSimulada();
    }

    private final Listener listener;

    public SimulacionNovedadesReceiver(Listener listener) {
        this.listener = listener;
    }

    public static IntentFilter crearFiltro() {
        IntentFilter filtro = new IntentFilter();
        filtro.addAction(ACCION_BAJA_PRECIO);
        filtro.addAction(ACCION_PUBLICACION_NUEVA);
        return filtro;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String accion = intent.getAction();
        if (ACCION_BAJA_PRECIO.equals(accion)) {
            FavoritoRepositoryMock.getInstancia().simularCambioDePrecio();
        } else if (ACCION_PUBLICACION_NUEVA.equals(accion)) {
            String titulo = intent.getStringExtra(EXTRA_TITULO);
            Categoria categoria = leerCategoria(intent.getStringExtra(EXTRA_CATEGORIA));
            BusquedaGuardadaRepositoryMock.getInstancia().simularPublicacionNueva(titulo, categoria);
        } else {
            return;
        }
        listener.onNovedadSimulada();
    }

    /** Categoria.valueOf tira si el extra no matchea ningún nombre del enum: se ignora en vez de crashear el broadcast. */
    private Categoria leerCategoria(String extra) {
        if (extra == null) {
            return null;
        }
        try {
            return Categoria.valueOf(extra.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException excepcion) {
            return null;
        }
    }
}
