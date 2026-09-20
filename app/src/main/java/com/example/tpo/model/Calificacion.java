package com.example.tpo.model;

import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Calificación que una parte le dejó a la otra después de una operación (Punto 9).
 * <p>
 * Es el insumo de la {@link Reputacion}: el promedio de estrellas sale de acá.
 * Guarda el título del artículo para que en el perfil público se entienda de qué
 * operación habla cada comentario sin tener que ir a buscarla.
 */
public class Calificacion implements Serializable {

    public static final int ESTRELLAS_MINIMAS = 1;
    public static final int ESTRELLAS_MAXIMAS = 5;

    /** "Comentario breve": mismo tope que valida el servidor. */
    public static final int LARGO_MAXIMO_COMENTARIO = 280;

    private final String id;
    private final String operacionId;
    private final UsuarioResumen autor;
    private final String calificadoId;
    private final String tituloArticulo;
    private final int estrellas;

    /** Opcional: {@code null} si el autor no escribió nada. */
    @Nullable
    private final String comentario;

    /** Momento en que se calificó, en milisegundos desde epoch. */
    private final long fecha;

    public Calificacion(String id,
                        String operacionId,
                        UsuarioResumen autor,
                        String calificadoId,
                        String tituloArticulo,
                        int estrellas,
                        @Nullable String comentario,
                        long fecha) {
        this.id = id;
        this.operacionId = operacionId;
        this.autor = autor;
        this.calificadoId = calificadoId;
        this.tituloArticulo = tituloArticulo;
        this.estrellas = estrellas;
        this.comentario = comentario;
        this.fecha = fecha;
    }

    public String getId() {
        return id;
    }

    public String getOperacionId() {
        return operacionId;
    }

    public UsuarioResumen getAutor() {
        return autor;
    }

    public String getCalificadoId() {
        return calificadoId;
    }

    public String getTituloArticulo() {
        return tituloArticulo;
    }

    public int getEstrellas() {
        return estrellas;
    }

    @Nullable
    public String getComentario() {
        return comentario;
    }

    public boolean tieneComentario() {
        return comentario != null && !comentario.trim().isEmpty();
    }

    public long getFecha() {
        return fecha;
    }
}
