package com.example.tpo.data;

import com.example.tpo.model.Usuario;

/**
 * Fuente de datos de perfiles y reputación (Punto 2 del TPO).
 * <p>
 * Sigue la misma forma que {@link PublicacionRepository}: los Fragments dependen
 * de esta interfaz y no de una implementación concreta. Hoy la única
 * implementación es {@link PerfilRepositoryMock}; cuando exista la API_Rest se
 * agrega una con Retrofit y las pantallas no se tocan.
 * <p>
 * Todas las operaciones son asincrónicas y responden por callback en el Main
 * Thread.
 */
public interface PerfilRepository {

    /**
     * Trae el perfil completo del usuario logueado.
     * <p>
     * Contra la API real esto sería {@code GET /perfil}: el servidor sabe quién
     * pide por el token, así que no hace falta mandarle el id.
     */
    void obtenerMiPerfil(RepositorioCallback<Usuario> callback);

    /**
     * Guarda los datos personales editados.
     * <p>
     * Devuelve el usuario tal como quedó guardado y no un simple "ok": si el
     * servidor normaliza algo (recorta espacios, pasa el email a minúsculas), la
     * pantalla tiene que mostrar lo que quedó del otro lado y no lo que el usuario
     * escribió. Equivale a un {@code PUT /perfil} que responde el recurso actualizado.
     *
     * @param usuario copia con los datos nuevos, armada con
     *                {@link Usuario#conDatosPersonales}.
     */
    void actualizarMiPerfil(Usuario usuario, RepositorioCallback<Usuario> callback);

    /**
     * Trae el perfil público de otra persona, para consultarlo antes de operar.
     * <p>
     * Equivale a {@code GET /usuarios/{id}}. Devuelve el mismo modelo que el
     * perfil propio; es la pantalla la que decide mostrar solo lo público
     * (reputación, antigüedad y nombre) y ocultar email y teléfono.
     *
     * @param usuarioId id de la persona que se quiere mirar.
     */
    void obtenerPerfilPublico(String usuarioId, RepositorioCallback<Usuario> callback);
}