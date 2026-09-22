package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** JSON del resumen más la lista de fotos. */
public class PublicacionDetalleResponse extends PublicacionResumenResponse {

    @SerializedName("fotos")
    public List<String> fotos;

    @Override
    protected int cantidadFotos() {
        return fotos != null ? fotos.size() : super.cantidadFotos();
    }
}
