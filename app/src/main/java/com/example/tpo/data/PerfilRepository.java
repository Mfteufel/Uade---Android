package com.example.tpo.data;

import android.graphics.Bitmap;
import android.net.Uri;

import com.example.tpo.model.Calificacion;
import com.example.tpo.model.Usuario;

import java.util.List;

/**
 * Fuente de datos de perfiles y reputación (Punto 2 del TPO).
 * <p>
 * Sigue la misma forma que {@link PublicacionRepository}: los Fragments dependen
 * de esta interfaz y no de una implementación concreta. Hilt decide cuál se
 * inyecta ({@code di/RepositoryModule}): hoy {@link PerfilRepositoryMock}; cuando
 * esté la API, {@link PerfilRepositoryApi}. Las pantallas no se tocan.
 * <p>
 * Todas las operaciones son asincrónicas y responden por callback en el Main
 * Thread. Los endpoints equivalentes están en
 * {@code docs/contrato-api-perfil-historial.md}.
 */
public interface PerfilRepository {

    /**
     * Trae el perfil completo del usuario logueado.
     * <p>
     * Equivale a {@code GET /usuarios/me}: el servidor sabe quién pide por el
     * token, así que no hace falta mandarle el id.
     */
    void obtenerMiPerfil(RepositorioCallback<Usuario> callback);

    /**
     * Guarda los datos personales editados.
     * <p>
     * Devuelve el usuario tal como quedó guardado y no un simple "ok": si el
     * servidor normaliza algo (recorta espacios, pasa el email a minúsculas), la
     * pantalla tiene que mostrar lo que quedó del otro lado y no lo que el usuario
     * escribió. Equivale a {@code PATCH /usuarios/me}.
     *
     * @param usuario copia con los datos nuevos, armada con
     *                {@link Usuario#conDatosPersonales}.
     */
    void actualizarMiPerfil(Usuario usuario, RepositorioCallback<Usuario> callback);

    /**
     * Trae el perfil público de otra persona, para consultarlo antes de operar.
     * <p>
     * Equivale a {@code GET /usuarios/{id}}. Devuelve el mismo modelo que el
     * perfil propio, pero sin email ni teléfono (vienen {@code null}).
     *
     * @param usuarioId id de la persona que se quiere mirar.
     */
    void obtenerPerfilPublico(String usuarioId, RepositorioCallback<Usuario> callback);

    /**
     * Reemplaza la foto de perfil del usuario logueado por la imagen elegida.
     * <p>
     * Recibe la {@link Uri} tal como la devuelve el selector de fotos: leerla,
     * achicarla y comprimirla es trabajo de disco y CPU que la implementación hace
     * fuera del Main Thread. Equivale a {@code PUT /usuarios/me/foto} (multipart).
     *
     * @return por callback, el perfil actualizado (ya con {@code fotoUrl}).
     */
    void actualizarFoto(Uri foto, RepositorioCallback<Usuario> callback);

    /**
     * Trae la foto de perfil de un usuario ya decodificada.
     * <p>
     * Solo tiene sentido si {@link Usuario#tieneFoto()}. Equivale a
     * {@code GET /usuarios/{id}/foto}.
     */
    void obtenerFoto(Usuario usuario, RepositorioCallback<Bitmap> callback);

    /**
     * Calificaciones que recibió un usuario, más recientes primero: lo que se
     * muestra en su perfil público. Equivale a {@code GET /usuarios/{id}/calificaciones}.
     */
    void obtenerCalificacionesRecibidas(String usuarioId,
                                        RepositorioCallback<List<Calificacion>> callback);
}
