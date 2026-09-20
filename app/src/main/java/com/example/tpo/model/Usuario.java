package com.example.tpo.model;

import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Un usuario de Ronda: sus datos personales más su reputación (Punto 2 del TPO).
 * <p>
 * La misma clase sirve para las dos pantallas del punto. En el perfil propio se
 * muestran todos los campos y son editables; en el perfil público de otra persona
 * se muestran solo la reputación, la antigüedad, el nombre y la foto. La
 * diferencia la hace la pantalla (y el servidor, que en el perfil público no manda
 * email ni teléfono), no el modelo: tener dos clases casi iguales sería duplicar
 * por nada.
 * <p>
 * Es inmutable, como {@link Publicacion}. Para editar se arma una copia con
 * {@link #conDatosPersonales} y se manda al repositorio; así no hay forma de que
 * la pantalla modifique el objeto y quede desincronizada de lo que el servidor
 * tiene guardado.
 * <p>
 * Implementa {@link Serializable} para poder viajar en un Bundle como argumento
 * de navegación hacia el perfil público.
 */
public class Usuario implements Serializable {

    private final String id;
    private final String nombre;

    /** Solo viene en el perfil propio; en el perfil público es {@code null}. */
    @Nullable
    private final String email;

    /**
     * Teléfono de contacto. Puede venir vacío: el enunciado no lo declara
     * obligatorio. En el perfil público es {@code null}.
     */
    @Nullable
    private final String telefono;

    /**
     * Zona declarada. Puede ser {@code null}: el backend crea usuarios por OTP sin
     * zona, y la pantalla tiene que tolerarlo en vez de romperse.
     */
    @Nullable
    private final Zona zona;

    /**
     * Alta en la plataforma, en milisegundos desde epoch.
     * <p>
     * Es lo que el enunciado llama "antigüedad". Se guarda la fecha y no los días
     * transcurridos porque los días cambian solos con el paso del tiempo; la fecha
     * de alta no. El cálculo se hace al mostrarla.
     * <p>
     * Se usa long y no java.time por el mismo motivo que en {@link Publicacion}:
     * el minSdk del proyecto es 24 y java.time necesita API 26.
     */
    private final long fechaAlta;

    private final Reputacion reputacion;

    /**
     * Dirección de la foto de perfil, relativa a la API (por ejemplo
     * {@code "usuarios/42/foto"}), o {@code null} si no cargó ninguna. La imagen se
     * pide aparte con {@code PerfilRepository.obtenerFoto}: así este objeto sigue
     * siendo liviano y viaja en un Bundle sin cargar bytes.
     */
    @Nullable
    private final String fotoUrl;

    /**
     * Operaciones propias que todavía se pueden calificar. Solo tiene sentido en
     * el perfil propio (es el aviso de "tenés calificaciones pendientes"); en el
     * perfil público vale 0.
     */
    private final int calificacionesPendientes;

    public Usuario(String id,
                   String nombre,
                   @Nullable String email,
                   @Nullable String telefono,
                   @Nullable Zona zona,
                   long fechaAlta,
                   Reputacion reputacion,
                   @Nullable String fotoUrl,
                   int calificacionesPendientes) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.zona = zona;
        this.fechaAlta = fechaAlta;
        this.reputacion = reputacion;
        this.fotoUrl = fotoUrl;
        this.calificacionesPendientes = calificacionesPendientes;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    @Nullable
    public String getTelefono() {
        return telefono;
    }

    @Nullable
    public Zona getZona() {
        return zona;
    }

    public long getFechaAlta() {
        return fechaAlta;
    }

    public Reputacion getReputacion() {
        return reputacion;
    }

    @Nullable
    public String getFotoUrl() {
        return fotoUrl;
    }

    public boolean tieneFoto() {
        return fotoUrl != null;
    }

    public int getCalificacionesPendientes() {
        return calificacionesPendientes;
    }

    /** Versión mínima del usuario (id + nombre), para operaciones y calificaciones. */
    public UsuarioResumen comoResumen() {
        return new UsuarioResumen(id, nombre);
    }

    /**
     * Devuelve una copia con los datos personales cambiados.
     * <p>
     * Solo se pueden tocar los cuatro campos que el enunciado declara editables.
     * El id, la fecha de alta, la reputación y la foto se arrastran de este objeto:
     * no se editan desde el formulario, y dejarlos fuera de la firma hace imposible
     * modificarlos por error desde la pantalla. La foto tiene su propio camino
     * ({@code PerfilRepository.actualizarFoto}).
     */
    public Usuario conDatosPersonales(String nuevoNombre,
                                      String nuevoEmail,
                                      String nuevoTelefono,
                                      Zona nuevaZona) {
        return new Usuario(id, nuevoNombre, nuevoEmail, nuevoTelefono, nuevaZona,
                fechaAlta, reputacion, fotoUrl, calificacionesPendientes);
    }
}
