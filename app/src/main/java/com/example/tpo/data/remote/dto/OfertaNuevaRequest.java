package com.example.tpo.data.remote.dto;

import androidx.annotation.Nullable;

/** Body del {@code POST /ofertas} que crea una oferta nueva. */
public class OfertaNuevaRequest {

    private final String publicacionId;
    private final double precio;
    @Nullable
    private final String mensaje;

    public OfertaNuevaRequest(String publicacionId, double precio, @Nullable String mensaje) {
        this.publicacionId = publicacionId;
        this.precio = precio;
        this.mensaje = mensaje;
    }

    public String getPublicacionId() {
        return publicacionId;
    }

    public double getPrecio() {
        return precio;
    }

    @Nullable
    public String getMensaje() {
        return mensaje;
    }
}
