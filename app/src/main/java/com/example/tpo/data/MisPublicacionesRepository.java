package com.example.tpo.data;

import com.example.tpo.model.BorradorPublicacion;
import com.example.tpo.model.MiPublicacion;

import java.util.List;

/**
 * Alta y gestión de las publicaciones del vendedor logueado (Punto 5).
 * <p>
 * Agrupa en una sola interfaz "publicar" y "Mis publicaciones" porque las
 * cuatro operaciones son el mismo ciclo de vida (crear, listar, pausar,
 * reactivar) de las publicaciones de un mismo usuario. La única implementación
 * es {@link MisPublicacionesRepositoryApi}, con Retrofit; el día que haya que
 * mockear esto para tests alcanza con crear otra implementación de esta
 * interfaz, como ya pasa con {@code PublicacionRepository} en el Home.
 */
public interface MisPublicacionesRepository {

    /** Sube el borrador completo (con fotos) y crea la publicación. */
    void publicar(BorradorPublicacion borrador, RepositorioCallback<MiPublicacion> callback);

    /** Publicaciones del usuario logueado, sin importar su estado. */
    void listar(RepositorioCallback<List<MiPublicacion>> callback);

    /** Pausa una publicación activa. */
    void pausar(String idPublicacion, RepositorioCallback<Void> callback);

    /** Reactiva una publicación pausada. */
    void reactivar(String idPublicacion, RepositorioCallback<Void> callback);
}
