package com.example.tpo.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NovedadesDePrecio {

    private static final Map<String, Double> reconocidos = new ConcurrentHashMap<>();

    private NovedadesDePrecio() {
        // Clase de utilidades: no se instancia.
    }

    /** El usuario ya vio que esta publicación tiene este precio. */
    public static void reconocer(String publicacionId, double precio) {
        reconocidos.put(publicacionId, precio);
    }

    /** true si el precio actual ya es el que se le mostró al usuario, por cualquiera de las dos secciones. */
    public static boolean estaReconocido(String publicacionId, double precioActual) {
        Double reconocido = reconocidos.get(publicacionId);
        return reconocido != null && reconocido == precioActual;
    }
}
