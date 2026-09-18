package com.example.tpo.data;

import com.example.tpo.model.Zona;
import com.example.tpo.model.Usuario;


/**
 * Sesión del usuario logueado, guardada en memoria.
 * <p>
 * Simplificación acordada para las primeras entregas: los datos viven en un
 * singleton y se pierden al cerrar la app. Cuando la cátedra vea
 * {@code SharedPreferences} hay que persistir esto de verdad.
 * <p>
 * Guarda solo lo mínimo para saber <i>quién</i> está usando la app: el id, y una
 * copia del nombre y la zona para no tener que ir al repositorio cada vez que
 * alguien los necesita. El perfil completo (email, teléfono, reputación) vive en
 * {@link PerfilRepository}; esta clase no es la fuente de verdad de esos datos.
 * <p>
 * El Home la usa para resolver el filtro de cercanía del Punto 3. El Punto 1
 * (Autenticación) es el que va a completar el login real y setear el id.
 */
public class SesionUsuario {

    private static SesionUsuario instancia;

    /**
     * Id del usuario logueado. Hasta que exista el Punto 1 arranca con el usuario
     * demo del repositorio de perfiles, para que la app tenga siempre alguien
     * logueado con quien trabajar.
     */
    private String usuarioId = PerfilRepositoryMock.ID_USUARIO_DEMO;


    /**
     * Zona declarada por el usuario. Hasta que exista el login se arranca con un
     * valor por defecto para que el filtro de cercanía sea probable.
     */
    private Zona zona = Zona.CABALLITO;

    private String nombre = "Invitado";

    private SesionUsuario() {
        // Constructor privado: se accede siempre por getInstancia().
    }

    public static synchronized SesionUsuario getInstancia() {
        if (instancia == null) {
            instancia = new SesionUsuario();
        }
        return instancia;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
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

    /**
     * Sincroniza la copia local con el perfil que quedó guardado.
     * <p>
     * Lo llama el repositorio de perfil después de una edición exitosa. Sin esto,
     * el usuario cambia su zona en el perfil y el Home sigue filtrando por
     * cercanía con la zona anterior hasta que se reinicie la app.
     */
    public void actualizarDesdePerfil(Usuario usuario) {
        if (usuario == null) return;
        this.usuarioId = usuario.getId();
        this.nombre = usuario.getNombre();
        this.zona = usuario.getZona();
    }

}
