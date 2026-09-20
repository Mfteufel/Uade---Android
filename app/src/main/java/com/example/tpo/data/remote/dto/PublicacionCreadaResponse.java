package com.example.tpo.data.remote.dto;

/**
 * Respuesta de la API al crear una publicación.
 * <p>
 * Solo se necesita el id que asigna el servidor; el resto de los datos ya los
 * tiene la app porque los mandó ella misma en el request.
 */
public class PublicacionCreadaResponse {

    private String id;

    public String getId() {
        return id;
    }
}
