package com.example.tpo.data;

import com.example.tpo.model.Zona;

/**
 * Sesión del usuario logueado, guardada en memoria.
 * <p>
 * Simplificación acordada para las primeras entregas: los datos viven en un
 * singleton y se pierden al cerrar la app. Cuando la cátedra vea
 * {@code SharedPreferences} hay que persistir esto de verdad.
 * <p>
 * El Home la usa solo para una cosa: saber la zona del usuario y poder resolver
 * el filtro de cercanía del Punto 3. El Punto 1 (Autenticación) es el que va a
 * completar esta clase con el email, el token y el resto de los datos reales.
 */
public class SesionUsuario {

    private static SesionUsuario instancia;

    /**
     * Zona declarada por el usuario. Hasta que exista el login se arranca con un
     * valor por defecto para que el filtro de cercanía sea probable.
     */
    private Zona zona = Zona.CABALLITO;

    /**
     * Nombre del usuario logueado. Hasta que exista el login se arranca con el
     * nombre de un vendedor del catálogo de prueba ("Martina G.", publicación
     * "1"): así el Detalle (Punto 4) puede mostrar la vista de "acciones según
     * rol" del vendedor sin necesitar todavía un id de usuario real.
     */
    private String nombre = "Martina G.";

    private SesionUsuario() {
        // Constructor privado: se accede siempre por getInstancia().
    }

    public static synchronized SesionUsuario getInstancia() {
        if (instancia == null) {
            instancia = new SesionUsuario();
        }
        return instancia;
    }

    public Zona getZona() {
        return zona;
    }

    public void setZona(Zona zona) {
        this.zona = zona;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
