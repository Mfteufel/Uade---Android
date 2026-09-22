package com.example.tpo.data;

import com.example.tpo.model.BusquedaGuardada;
import com.example.tpo.model.FiltroPublicaciones;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BusquedaGuardadaRepository {

    void precargar(RepositorioCallback<Void> callback);

    void guardar(String nombre, FiltroPublicaciones filtro, RepositorioCallback<Void> callback);

    /** Búsquedas guardadas del usuario, la más reciente primero. */
    void listar(RepositorioCallback<List<BusquedaGuardada>> callback);

    void eliminar(String id, RepositorioCallback<Void> callback);

    /** true si esta búsqueda tiene publicaciones nuevas sin ver. */
    boolean tieneNovedad(String id);

    /** true si hay alguna novedad sin ver entre todas las búsquedas guardadas. */
    boolean hayAlgunaNovedad();

    /**
     * Limpia la novedad pendiente de una búsqueda puntual — se llama cuando el
     * usuario realmente la aplica (entra a ver sus resultados), no por abrir o
     * cerrar la hoja de "Búsquedas guardadas" sin elegir nada.
     */
    void marcarVisto(String id);

    /** IDs de las publicaciones nuevas que matchean esta búsqueda, para destacarlas en el listado. */
    Set<String> publicacionesNuevasDe(String id);

    /**
     * De las publicaciones que matchean esta búsqueda, cuáles cambiaron de
     * precio desde la última vez que se revisó — id de publicación -> true si
     * subió, false si bajó. Para destacarlas en el listado igual que las nuevas.
     */
    Map<String, Boolean> publicacionesConCambioDePrecioDe(String id);
}
