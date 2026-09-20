package com.example.tpo.model;

import java.io.Serializable;

/**
 * Lo mínimo para identificar a una persona dentro de otro recurso: id y nombre.
 * <p>
 * Es lo que viaja como comprador, vendedor o contraparte de una {@link Operacion}
 * y como autor de una {@link Calificacion}. No se usa {@link Usuario} ahí porque
 * arrastraría email, teléfono y reputación, que la API no manda (ni debe mandar)
 * dentro de una operación ajena. Con el id alcanza para abrir su perfil público.
 */
public class UsuarioResumen implements Serializable {

    private final String id;
    private final String nombre;

    public UsuarioResumen(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
