package com.example.tpo.data.remote.dto;

import com.example.tpo.model.Usuario;
import com.google.gson.annotations.SerializedName;

/** Cuerpo de {@code PATCH /usuarios/me}: los cuatro datos editables. */
public class ActualizarPerfilRequest {

    @SerializedName("nombre")
    public final String nombre;

    @SerializedName("email")
    public final String email;

    @SerializedName("telefono")
    public final String telefono;

    /** Nombre del enum Zona ("CABALLITO"). */
    @SerializedName("zona")
    public final String zona;

    public ActualizarPerfilRequest(Usuario editado) {
        this.nombre = editado.getNombre();
        this.email = editado.getEmail();
        this.telefono = editado.getTelefono();
        this.zona = editado.getZona() == null ? null : editado.getZona().name();
    }
}
