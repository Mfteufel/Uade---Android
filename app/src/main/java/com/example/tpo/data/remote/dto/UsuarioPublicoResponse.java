package com.example.tpo.data.remote.dto;

import androidx.annotation.Nullable;

import com.example.tpo.model.Zona;
import com.google.gson.annotations.SerializedName;

/**
 * Identidad del usuario logueado, tal como la devuelven {@code /auth/*} y
 * {@code GET /auth/sesion}. Es más chica que {@code UsuarioResponse} (sin
 * teléfono ni reputación): alcanza para hidratar {@code SesionUsuario}.
 * <p>
 * {@code id} es String aunque el backend use enteros, igual que en
 * {@code UsuarioResponse}.
 */
public class UsuarioPublicoResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("email")
    public String email;

    @SerializedName("nombre")
    public String nombre;

    /** Nombre del enum {@link Zona} ("CABALLITO"), o null. */
    @SerializedName("zona")
    public String zona;

    /** Una zona desconocida o ausente queda en null: quien la use conserva la anterior. */
    @Nullable
    public Zona zonaModelo() {
        if (zona == null) {
            return null;
        }
        try {
            return Zona.valueOf(zona);
        } catch (IllegalArgumentException excepcion) {
            return null;
        }
    }
}
