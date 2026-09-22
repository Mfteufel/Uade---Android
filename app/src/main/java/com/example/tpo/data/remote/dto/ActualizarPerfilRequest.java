package com.example.tpo.data.remote.dto;

import com.example.tpo.model.Usuario;
import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de {@code PUT /usuarios/yo}: los cuatro datos editables.
 * <p>
 * El PUT reemplaza el perfil completo, así que siempre viajan los cuatro. Un
 * teléfono vacío se manda como null: Gson omite el campo y el backend lo guarda
 * en null, que es como queda un usuario que nunca cargó teléfono.
 */
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
        String telefonoEditado = editado.getTelefono();
        this.telefono = telefonoEditado == null || telefonoEditado.trim().isEmpty()
                ? null : telefonoEditado;
        this.zona = editado.getZona() == null ? null : editado.getZona().name();
    }
}
