package com.example.tpo.data;

import com.example.tpo.model.Pregunta;

import java.util.List;

/**
 * Preguntas que los interesados le hacen al vendedor sobre una publicación —
 * Punto 4 del TPO, contra el backend real.
 */
public interface PreguntaRepository {

    /** Todas las preguntas de una publicación, más antigua primero. */
    void deLaPublicacion(String publicacionId, RepositorioCallback<List<Pregunta>> callback);

    /** Envía una pregunta nueva sobre esta publicación, en nombre del usuario logueado. */
    void crear(String publicacionId, String texto, RepositorioCallback<Pregunta> callback);

    /** Contesta una pregunta recibida. Solo el dueño de la publicación puede hacerlo (el backend lo valida). */
    void responder(String publicacionId, String preguntaId, String texto, RepositorioCallback<Pregunta> callback);
}
