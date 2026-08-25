package com.example.tpo.data;

/**
 * Callback genérico para las operaciones del repositorio.
 * <p>
 * Tiene a propósito la misma forma que el {@code Callback} de Retrofit
 * (un camino de éxito y uno de error, ambos invocados en el Main Thread) para que
 * el día que exista la API_Rest del TPO el cambio sea reemplazar la implementación
 * del repositorio por una que use {@code call.enqueue(...)}, sin tocar la UI.
 *
 * @param <T> tipo del resultado devuelto en caso de éxito.
 */
public interface RepositorioCallback<T> {

    /** Se invoca en el Main Thread con el resultado listo para mostrar. */
    void onExito(T resultado);

    /**
     * Se invoca en el Main Thread cuando la operación falla.
     *
     * @param mensaje texto ya listo para mostrarle al usuario.
     */
    void onError(String mensaje);
}
