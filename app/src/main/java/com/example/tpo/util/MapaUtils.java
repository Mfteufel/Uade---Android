package com.example.tpo.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

/**
 * "Cómo llegar" del Punto 8: abre la app de mapas del dispositivo con la
 * dirección de entrega ya cargada, sin embeber ningún mapa dentro de Ronda
 * (el profesor lo aclaró en la clase remota: solo Intent implícito).
 * <p>
 * La dirección es texto libre (puede ser "Av. Corrientes 1234" o directamente
 * coordenadas como "-34.60,-58.38"): no se valida ni se geocodifica acá, tal
 * cual pidió la consigna.
 */
public final class MapaUtils {

    private MapaUtils() {
        // Clase de utilidades: no se instancia.
    }

    /** true si hay algo cargado en el campo de dirección (no null, no vacío ni solo espacios). */
    public static boolean tieneDireccion(@Nullable String direccion) {
        return direccion != null && !direccion.trim().isEmpty();
    }

    /**
     * Intenta abrir la dirección en una app de mapas, probando primero la opción más
     * específica y cayendo a la siguiente si el dispositivo no tiene ninguna app que la
     * entienda. Devuelve false si ninguna de las tres funcionó, para que quien llama pueda
     * avisarle al usuario con un Snackbar.
     * <p>
     * No usamos {@code Uri.Builder} para armar estas URIs: tanto {@code geo:} como
     * {@code google.navigation:} son URIs "opacas" (no tienen esquema://host/path como una URL
     * normal), y el Builder las arma con barras de más que rompen el esquema. Por eso se
     * concatena el string a mano con {@link Uri#encode}, que además escapa los espacios como
     * %20 (Maps no entiende el "+" que generaría URLEncoder).
     */
    public static boolean abrirComoLlegar(Context contexto, String direccion) {
        String direccionCodificada = Uri.encode(direccion);

        // 1) google.navigation: es la que arranca Maps directo en modo navegación,
        //    que es literalmente lo que pide el enunciado. Solo la entiende Google Maps.
        if (intentarAbrir(contexto, "google.navigation:q=" + direccionCodificada)) {
            return true;
        }
        // 2) geo: es el esquema estándar de Android para ubicaciones: lo entiende
        //    cualquier app de mapas que el usuario tenga puesta como predeterminada,
        //    no solo Google Maps (el enunciado pide contemplar esto también).
        if (intentarAbrir(contexto, "geo:0,0?q=" + direccionCodificada)) {
            return true;
        }
        // 3) Último recurso: el link web de Maps. Si no hay ninguna app de mapas
        //    instalada, esto al menos abre en el navegador.
        return intentarAbrir(contexto,
                "https://www.google.com/maps/dir/?api=1&destination=" + direccionCodificada);
    }

    private static boolean intentarAbrir(Context contexto, String uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            // FLAG_ACTIVITY_NEW_TASK porque a veces el contexto que llega acá no es
            // el de una Activity (por ejemplo si en el futuro esto se llama desde un
            // Service o un BroadcastReceiver); no hace daño pasarlo siempre.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            contexto.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            // No hay ninguna app instalada que resuelva esta URI puntual. No es un
            // error real: es el caso esperado en un emulador sin Google Maps, así
            // que simplemente probamos con la siguiente opción de la cascada.
            return false;
        }
    }
}
