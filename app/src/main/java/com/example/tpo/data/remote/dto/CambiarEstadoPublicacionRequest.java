package com.example.tpo.data.remote.dto;

/** Body del PATCH que pausa o reactiva una publicación. */
public class CambiarEstadoPublicacionRequest {

    private final String estadoPublicacion;

    public CambiarEstadoPublicacionRequest(String estadoPublicacion) {
        this.estadoPublicacion = estadoPublicacion;
    }

    public String getEstadoPublicacion() {
        return estadoPublicacion;
    }
}
