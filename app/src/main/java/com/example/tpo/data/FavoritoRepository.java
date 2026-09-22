package com.example.tpo.data;

import com.example.tpo.model.Publicacion;

import java.util.List;

public interface FavoritoRepository {

    void precargar(RepositorioCallback<Void> callback);

    boolean esFavorito(String publicacionId);


    void marcar(Publicacion publicacion, RepositorioCallback<Void> callback);

    void desmarcar(String publicacionId, RepositorioCallback<Void> callback);

    /** Publicaciones favoritas del usuario, la más reciente marcada primero. */
    void listar(RepositorioCallback<List<Publicacion>> callback);

    /** true si esta publicación favorita tiene una novedad sin ver (Punto 10). */
    boolean tieneNovedad(String publicacionId);

    /**
     * true si el precio subió desde que se marcó como favorita; false si bajó.
     */
    boolean subioDePrecio(String publicacionId);

    /** true si hay alguna novedad sin ver entre todos los favoritos. */
    boolean hayAlgunaNovedad();

    /** Limpia todas las novedades pendientes porque el usuario ya las vio. */
    void marcarTodoVisto();
}
