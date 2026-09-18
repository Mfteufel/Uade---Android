package com.example.tpo.model;

import java.io.Serializable;

/**
 * Un usuario de Ronda: sus datos personales más su reputación (Punto 2 del TPO).
 * <p>
 * La misma clase sirve para las dos pantallas del punto. En el perfil propio se
 * muestran todos los campos y son editables; en el perfil público de otra persona
 * se muestran solo la reputación, la antigüedad y el nombre. La diferencia la hace
 * la pantalla, no el modelo: tener dos clases casi iguales sería duplicar por nada.
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
    private final String email;

    /** Teléfono de contacto. Puede venir vacío: el enunciado no lo declara obligatorio. */
    private final String telefono;

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

    public Usuario(String id,
                   String nombre,
                   String email,
                   String telefono,
                   Zona zona,
                   long fechaAlta,
                   Reputacion reputacion) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.zona = zona;
        this.fechaAlta = fechaAlta;
        this.reputacion = reputacion;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public Zona getZona() {
        return zona;
    }

    public long getFechaAlta() {
        return fechaAlta;
    }

    public Reputacion getReputacion() {
        return reputacion;
    }

    /**
     * Devuelve una copia con los datos personales cambiados.
     * <p>
     * Solo se pueden tocar los cuatro campos que el enunciado declara editables.
     * El id, la fecha de alta y la reputación se arrastran de este objeto: no son
     * del usuario, son del sistema, y dejarlos fuera de la firma hace imposible
     * modificarlos por error desde la pantalla.
     */
    public Usuario conDatosPersonales(String nuevoNombre,
                                      String nuevoEmail,
                                      String nuevoTelefono,
                                      Zona nuevaZona) {
        return new Usuario(id, nuevoNombre, nuevoEmail, nuevoTelefono, nuevaZona,
                fechaAlta, reputacion);
    }
}