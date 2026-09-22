package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class FavoritoResponse extends PublicacionResumenResponse {

    @SerializedName("precioAlGuardar")
    public double precioAlGuardar;

    @SerializedName("tieneNovedad")
    public boolean tieneNovedad;
}
