package com.example.tpo.data.remote.dto;

import com.example.tpo.model.UsuarioResumen;
import com.google.gson.annotations.SerializedName;

/** JSON {@code {"id", "nombre"}}: comprador, vendedor o autor dentro de otro recurso. */
public class UsuarioResumenResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("nombre")
    public String nombre;

    public UsuarioResumen aModelo() {
        return new UsuarioResumen(id, nombre);
    }
}
