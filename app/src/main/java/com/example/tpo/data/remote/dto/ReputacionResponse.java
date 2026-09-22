package com.example.tpo.data.remote.dto;

import com.example.tpo.model.Reputacion;
import com.google.gson.annotations.SerializedName;

/**
 * JSON de la reputación, tal como la calcula el servidor. Ver
 * {@code docs/contrato-api-perfil-historial.md}.
 */
public class ReputacionResponse {

    @SerializedName("promedioEstrellas")
    public double promedio;

    @SerializedName("cantidad_calificaciones")
    public int cantidadCalificaciones;

    @SerializedName("operacionesComoComprador")
    public int operacionesComoComprador;

    @SerializedName("operacionesComoVendedor")
    public int operacionesComoVendedor;

    public Reputacion aModelo() {
        return new Reputacion(promedio, cantidadCalificaciones,
                operacionesComoComprador, operacionesComoVendedor);
    }

    /** Si el servidor no manda el bloque, se muestra como "sin actividad" en vez de romper. */
    public static Reputacion aModelo(ReputacionResponse json) {
        return json == null ? new Reputacion(0, 0, 0, 0) : json.aModelo();
    }
}
