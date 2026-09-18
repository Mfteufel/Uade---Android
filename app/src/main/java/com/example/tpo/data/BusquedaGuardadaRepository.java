package com.example.tpo.data;

import com.example.tpo.model.BusquedaGuardada;
import com.example.tpo.model.FiltroPublicaciones;

import java.util.List;
import java.util.Set;

public interface BusquedaGuardadaRepository {

    void guardar(String nombre, FiltroPublicaciones filtro, RepositorioCallback<Void> callback);

    /** Búsquedas guardadas del usuario, la más reciente primero. */
    void listar(RepositorioCallback<List<BusquedaGuardada>> callback);

    void eliminar(String id, RepositorioCallback<Void> callback);

    /** true si esta búsqueda tiene publicaciones nuevas sin ver. */
    boolean tieneNovedad(String id);

    /** true si hay alguna novedad sin ver entre todas las búsquedas guardadas. */
    boolean hayAlgunaNovedad();

    /** Limpia todas las novedades pendientes cuando el usuario ya las vio. */
    void marcarTodoVisto();

    /** IDs de las publicaciones nuevas que matchean esta búsqueda, para destacarlas en el listado. */
    Set<String> publicacionesNuevasDe(String id);
}
