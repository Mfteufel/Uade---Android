package com.example.tpo.data.remote.dto;

/** Body del {@code PATCH /ofertas/{id}/estado}: acepta o rechaza. */
public class CambioEstadoOfertaRequest {

    private final String estado;

    public CambioEstadoOfertaRequest(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return estado;
    }
}
