package com.example.tpo.data.remote.dto;

import com.example.tpo.model.Usuario;
import com.example.tpo.model.Zona;
import com.google.gson.annotations.SerializedName;

/**
 * JSON de un usuario: el perfil propio ({@code GET /usuarios/yo}) o el público
 * ({@code GET /usuarios/{id}}, sin email ni teléfono). Ver
 * {@code docs/contrato-api-perfil-historial.md}.
 * <p>
 * {@code id} es String aunque el backend use enteros: Gson acepta un número JSON
 * en un campo String, y la app ya maneja todos los ids como texto (argumentos de
 * navegación, sesión).
 */
public class UsuarioResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("email")
    public String email;

    @SerializedName("telefono")
    public String telefono;

    /** Nombre del enum {@link Zona} ("CABALLITO"), o null. */
    @SerializedName("zona")
    public String zona;

    /** Epoch en milisegundos. */
    @SerializedName("fechaAlta")
    public long fechaAlta;

    @SerializedName("fotoUrl")
    public String fotoUrl;

    @SerializedName("reputacion")
    public ReputacionResponse reputacion;

    /** Solo viene en el perfil propio. */
    @SerializedName("calificacionesPendientes")
    public Integer calificacionesPendientes;

    public Usuario aModelo() {
        return new Usuario(id, nombre, email, telefono, zonaDe(zona), fechaAlta,
                ReputacionResponse.aModelo(reputacion), fotoUrl,
                calificacionesPendientes == null ? 0 : calificacionesPendientes);
    }

    /** Una zona desconocida o ausente queda en null: la pantalla lo tolera y pide elegirla. */
    private static Zona zonaDe(String nombre) {
        if (nombre == null) {
            return null;
        }
        try {
            return Zona.valueOf(nombre);
        } catch (IllegalArgumentException excepcion) {
            return null;
        }
    }
}
