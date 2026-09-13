package com.example.tpo.data;

import com.example.tpo.model.FiltroPublicaciones;
import com.example.tpo.model.Publicacion;

/**
 * Fuente de datos de publicaciones para el Home.
 * <p>
 * El Fragment depende de esta interfaz y no de una implementación concreta. Hoy
 * la única implementación es {@link PublicacionRepositoryMock} (datos fijos en
 * memoria, porque la API_Rest del TPO todavía no existe); cuando el backend esté
 * listo se agrega una implementación con Retrofit y el Home no se toca.
 * <p>
 * Todas las operaciones son asincrónicas y responden por callback: nunca hay que
 * bloquear el Main Thread esperando datos.
 */
public interface PublicacionRepository {

    /**
     * Cantidad de publicaciones por página.
     * <p>
     * El enunciado pide "listado paginado". Se mantiene chico a propósito para
     * que la paginación se note al hacer scroll con el set de datos de prueba.
     */
    int TAMANIO_PAGINA = 8;

    /**
     * Busca publicaciones aplicando texto, filtros y ordenamiento.
     *
     * @param filtro   criterios de búsqueda (texto, categoría, estado, precio, cercanía, orden).
     * @param pagina   página pedida, empezando en 0.
     * @param callback dónde se avisa el resultado. Siempre se invoca en el Main Thread.
     */
    void buscarPublicaciones(FiltroPublicaciones filtro,
                             int pagina,
                             RepositorioCallback<PaginaPublicaciones> callback);

    /**
     * Busca una publicación puntual por id, para la pantalla de Detalle (Punto 4).
     *
     * @param id       id de la publicación ({@link Publicacion#getId()}).
     * @param callback dónde se avisa el resultado. Siempre se invoca en el Main Thread.
     */
    void obtenerPublicacion(String id, RepositorioCallback<Publicacion> callback);

    /**
     * Trae el perfil público de un vendedor: sus datos y sus publicaciones
     * activas, para la pantalla de Perfil (Punto 4).
     * <p>
     * Va en una sola operación y no en dos (datos + publicaciones) porque la
     * pantalla las necesita juntas: con dos callbacks habría que coordinar dos
     * estados de carga y decidir qué hacer si una falla y la otra no.
     *
     * @param vendedorId id del vendedor ({@link com.example.tpo.model.Vendedor#getId()}).
     * @param callback   dónde se avisa el resultado. Siempre se invoca en el Main Thread.
     */
    void obtenerPerfilVendedor(String vendedorId, RepositorioCallback<PerfilVendedor> callback);
}
