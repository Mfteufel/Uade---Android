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
     * nombre de un vendedor del catálogo de prueba ("Martina G."): así el Detalle
     * (Punto 4) puede mostrar la vista de "acciones según rol" del vendedor sin
     * necesitar todavía un login real.
     */
    private String nombre = "Martina G.";

    /**
     * Id del usuario logueado. Coincide con el id del vendedor "Martina G." en el
     * catálogo mock ({@code PublicacionRepositoryMock}), que es dueño de varias
     * publicaciones: así el Detalle puede comparar por id (y no por nombre, que es
     * frágil ante homónimos) para decidir si mostrás la vista de vendedor o la de
     * interesado. El Punto 1 (Autenticación) lo va a completar con el id real.
     */
    private String idUsuario = "v1";

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

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }
}
