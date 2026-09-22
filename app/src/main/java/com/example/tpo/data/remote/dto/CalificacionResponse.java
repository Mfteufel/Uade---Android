package com.example.tpo.data.remote.dto;

import com.example.tpo.model.Calificacion;
import com.google.gson.annotations.SerializedName;

/**
 * JSON de una calificación: la propia dentro de una operación
 * ({@code miCalificacion}) o cada una de {@code GET /usuarios/{id}/calificaciones}.
 */
public class CalificacionResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("operacionId")
    public String operacionId;

    @SerializedName("autor")
    public UsuarioResumenResponse autor;

    @SerializedName("calificadoId")
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
