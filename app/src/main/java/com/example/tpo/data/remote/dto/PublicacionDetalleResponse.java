package com.example.tpo.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** JSON del resumen más la lista de fotos y, si el que pregunta es el dueño, la dirección de entrega. */
public class PublicacionDetalleResponse extends PublicacionResumenResponse {

    @SerializedName("fotos")
    public List<String> fotos;

    /**
     * Solo viene con valor si quien pidió el detalle es el dueño de la
     * publicación (ver {@code routers/publicaciones.py}, {@code a_json}); para
     * cualquier otro caso el backend manda {@code null}, aunque la publicación
     * sí tenga una dirección cargada.
     */
    @SerializedName("direccionEntrega")
    public String direccionEntregaJson;

    @Override
    protected int cantidadFotos() {
        return fotos != null ? fotos.size() : super.cantidadFotos();
    }

    @Nullable
    @Override
    protected String direccionEntrega() {
        return direccionEntregaJson;
    }
}
