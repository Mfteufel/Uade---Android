package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** JSON de una página del listado. */
public class PaginaPublicacionesResponse {

    @SerializedName("publicaciones")
    public List<PublicacionResumenResponse> publicaciones;

    @SerializedName("pagina")
    public int pagina;

    @SerializedName("hayMas")
    public boolean hayMas;

    @SerializedName("totalResultados")
    public int totalResultados;
}
