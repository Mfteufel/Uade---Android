package com.example.tpo.data.remote.dto;

import com.example.tpo.model.Calificacion;
import com.google.gson.annotations.SerializedName;

/** JSON de una calificación. Ver {@code docs/contrato-api-perfil-historial.md}. */
public class CalificacionResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("operacion_id")
    public String operacionId;

    @SerializedName("autor")
    public UsuarioResumenResponse autor;

    @SerializedName("calificado_id")
    public String calificadoId;

    @SerializedName("articulo")
    public String articulo;

    @SerializedName("estrellas")
    public int estrellas;

    @SerializedName("comentario")
    public String comentario;

    /** Epoch en milisegundos. */
    @SerializedName("fecha")
    public long fecha;

    public Calificacion aModelo() {
        return new Calificacion(id, operacionId, autor.aModelo(), calificadoId, articulo,
                estrellas, comentario, fecha);
    }
}
