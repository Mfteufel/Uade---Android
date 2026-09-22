package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class BusquedaResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("filtro")
    public FiltroGuardadoDto filtro;

    @SerializedName("fechaGuardado")
    public long fechaGuardado;

    @SerializedName("cantidadNuevas")
    public int cantidadNuevas;
}
