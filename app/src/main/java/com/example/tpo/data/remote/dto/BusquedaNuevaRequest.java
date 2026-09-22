package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Body del POST que guarda una búsqueda nueva. */
public class BusquedaNuevaRequest {

    @SerializedName("nombre")
    public final String nombre;

    @SerializedName("filtro")
    public final FiltroGuardadoDto filtro;

    public BusquedaNuevaRequest(String nombre, FiltroGuardadoDto filtro) {
        this.nombre = nombre;
        this.filtro = filtro;
    }
}
