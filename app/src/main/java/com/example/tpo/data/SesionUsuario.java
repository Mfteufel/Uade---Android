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
    private String usuarioId = BaseDeDatosMock.ID_USUARIO_DEMO;


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

    // TODO: idUsuario (Punto 4, ids "v1".."v12" del catálogo mock) y usuarioId
    // (Punto 1/2, ids "u0".."u2" del catálogo de perfiles) modelan la misma
    // idea — quién está logueado — con dos catálogos separados. Quedan los
    // dos por ahora para no romper ninguno de los dos lados; unificarlos es
    // parte de la reconciliación pendiente del merge (ver PR).
    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
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
        // Un usuario creado por OTP puede no tener zona todavía: se conserva la
        // anterior para que el filtro de cercanía del Home no quede con null.
        if (usuario.getZona() != null) {
            this.zona = usuario.getZona();
        }
    }

    /**
     * Vuelve la sesión a los valores demo con los que arranca la app. La llama el
     * logout: sin esto, cerrar sesión borraría el token pero el nombre/id de la
     * sesión anterior seguirían pisando el mock hasta reiniciar el proceso.
     */
    public void limpiar() {
        this.usuarioId = BaseDeDatosMock.ID_USUARIO_DEMO;
        this.zona = Zona.CABALLITO;
        this.nombre = "Martina G.";
        this.idUsuario = "v1";
    }
}
