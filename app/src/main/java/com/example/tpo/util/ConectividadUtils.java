package com.example.tpo.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

/**
 * Chequeo de conectividad para el modo offline, para decidir si
 * pedirle datos al repositorio o directamente mostrar
 * lo que haya cacheado.
 */
public final class ConectividadUtils {

    private ConectividadUtils() {
    }

    public static boolean hayConexion(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        NetworkCapabilities capacidades = manager.getNetworkCapabilities(manager.getActiveNetwork());
        return capacidades != null
                && capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
