package com.example.tpo.data.remote.dto;

/** Body del {@code PATCH /ofertas/{id}/precio}: contraoferta. */
public class CambioPrecioOfertaRequest {

    private final double precio;

    public CambioPrecioOfertaRequest(double precio) {
        this.precio = precio;
    }

    public double getPrecio() {
        return precio;
    }
}
