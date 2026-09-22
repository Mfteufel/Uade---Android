package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** JSON de una página de {@code GET /publicaciones}. */
public class PaginaPublicacionesResponse {

    @SerializedName("publicaciones")
    public List<PublicacionResponse> publicaciones;

    /** Número de página devuelta, empezando en 0. */
    @SerializedName("pagina")
    public int pagina;

    @SerializedName("hayMas")
    public boolean hayMas;

    @SerializedName("totalResultados")
    public int totalResultados;
}
