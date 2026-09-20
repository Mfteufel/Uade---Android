package com.example.tpo.data;

import com.example.tpo.model.BorradorPublicacion;

/**
 * Persistencia del borrador de "Publicar artículo" (Punto 5).
 * <p>
 * El enunciado pide que si el usuario cierra la app a mitad de carga, al volver
 * encuentre el borrador donde lo dejó. Esta interfaz es lo único de lo que
 * depende {@link com.example.tpo.ui.publicar.PublicarArticuloViewModel}; la
 * única implementación hoy es {@link BorradorRepositoryLocal}, que usa Room.
 */
public interface BorradorRepository {

    /**
     * Guarda (o reemplaza) el borrador vigente. Se llama después de cada paso del
     * wizard, así que es intencionalmente "fire and forget": no hace falta avisar
     * a la UI de que se guardó.
     */
    void guardar(BorradorPublicacion borrador);

    /**
     * Busca el borrador guardado.
     *
     * @param callback recibe {@code null} en {@code onExito} si no hay ningún
     *                 borrador pendiente (caso normal la primera vez que se
     *                 abre el wizard).
     */
    void obtener(RepositorioCallback<BorradorPublicacion> callback);

    /** Borra el borrador. Se llama una vez que la publicación se creó con éxito. */
    void borrar();
}
