package com.example.tpo.data;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.tpo.data.remote.ErrorApi;
import com.example.tpo.data.remote.UsuarioApi;
import com.example.tpo.data.remote.dto.ActualizarPerfilRequest;
import com.example.tpo.data.remote.dto.CalificacionResponse;
import com.example.tpo.data.remote.dto.PaginaPublicacionesResponse;
import com.example.tpo.data.remote.dto.PublicacionResponse;
import com.example.tpo.data.remote.dto.UsuarioResponse;
import com.example.tpo.model.Calificacion;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Usuario;
import com.example.tpo.util.ImagenUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Implementación de {@link PerfilRepository} contra la API REST, con Retrofit.
 * <p>
 * Se activa en {@code di/RepositoryModule}. Las pantallas no cambian: reciben los
 * mismos modelos y los mismos mensajes de error por {@link RepositorioCallback}
 * que con {@link PerfilRepositoryMock}.
 * <p>
 * El backend todavía no tiene foto de perfil: para eso se responde localmente,
 * sin pedir nada al servidor.
 * <p>
 * No construye Retrofit: recibe {@link UsuarioApi} ya creada desde el único
 * {@code Retrofit} de {@code NetworkModule}, que agrega el token JWT.
 */
public class PerfilRepositoryApi implements PerfilRepository {

    /**
     * Tope de páginas al juntar las publicaciones de un vendedor: el perfil las
     * muestra todas en una lista, pero no tiene sentido seguir pidiendo sin fin si
     * el servidor respondiera mal {@code hayMas}.
     */
    private static final int MAXIMO_PAGINAS_PUBLICACIONES = 10;

    private final UsuarioApi api;
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());
    /** Para decodificar fotos fuera del Main Thread. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PerfilRepositoryApi(UsuarioApi api) {
        this.api = api;
    }

    @Override
    public void obtenerMiPerfil(RepositorioCallback<Usuario> callback) {
        api.obtenerMiPerfil().enqueue(
                adaptar(callback, "No pudimos cargar tu perfil", PerfilRepositoryApi::perfilPropio));
    }

    @Override
    public void actualizarMiPerfil(Usuario usuario, RepositorioCallback<Usuario> callback) {
        // El backend todavía no valida formato: sin esto guardaría un email sin @
        // o un teléfono con letras.
        String errorValidacion = ValidadorPerfil.validar(usuario);
        if (errorValidacion != null) {
            handlerPrincipal.post(() -> callback.onError(errorValidacion));
            return;
        }
        Usuario normalizado = ValidadorPerfil.normalizar(usuario);
        api.actualizarMiPerfil(new ActualizarPerfilRequest(normalizado)).enqueue(
                adaptar(callback, "No pudimos guardar los cambios", PerfilRepositoryApi::perfilPropio));
    }

    @Override
    public void obtenerPerfilPublico(String usuarioId, RepositorioCallback<Usuario> callback) {
        api.obtenerPerfilPublico(usuarioId).enqueue(
                adaptar(callback, "No pudimos cargar el perfil", UsuarioResponse::aModelo));
    }

    @Override
    public void obtenerPublicacionesActivas(String usuarioId,
                                            RepositorioCallback<List<Publicacion>> callback) {
        pedirPaginaDePublicaciones(usuarioId, 0, new ArrayList<>(), callback);
    }

    @Override
    public boolean permiteCambiarFoto() {
        return false;
    }

    /** El backend no tiene endpoint de foto: se contesta sin mandar nada. */
    @Override
    public void actualizarFoto(Uri foto, RepositorioCallback<Usuario> callback) {
        handlerPrincipal.post(() ->
                callback.onError("Por ahora no se puede cambiar la foto de perfil"));
    }

    @Override
    public void obtenerFoto(Usuario usuario, RepositorioCallback<Bitmap> callback) {
        String url = usuario.getFotoUrl();
        if (url == null) {
            handlerPrincipal.post(() -> callback.onError("Este usuario no tiene foto"));
            return;
        }
        // execute() (sincrónico) dentro del executor: la descarga y el decode
        // quedan los dos fuera del Main Thread, y el resultado vuelve por el Handler.
        executor.execute(() -> {
            try {
                Response<ResponseBody> respuesta = api.descargar(url).execute();
                ResponseBody cuerpo = respuesta.body();
                if (!respuesta.isSuccessful() || cuerpo == null) {
                    String mensaje = ErrorApi.mensaje(respuesta, "No pudimos cargar la foto");
                    handlerPrincipal.post(() -> callback.onError(mensaje));
                    return;
                }
                Bitmap imagen = ImagenUtils.decodificar(cuerpo.bytes());
                handlerPrincipal.post(() -> {
                    if (imagen == null) {
                        callback.onError("No pudimos mostrar la foto");
                    } else {
                        callback.onExito(imagen);
                    }
                });
            } catch (IOException excepcion) {
                handlerPrincipal.post(() -> callback.onError(ErrorApi.SIN_CONEXION));
            }
        });
    }

    @Override
    public void obtenerCalificacionesRecibidas(String usuarioId,
                                               RepositorioCallback<List<Calificacion>> callback) {
        api.obtenerCalificacionesRecibidas(usuarioId).enqueue(
                adaptar(callback, "No pudimos cargar las calificaciones", lista -> {
                    List<Calificacion> calificaciones = new ArrayList<>();
                    for (CalificacionResponse json : lista) {
                        calificaciones.add(json.aModelo());
                    }
                    return calificaciones;
                }));
    }

    // ---------------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------------

    /**
     * Perfil propio: además de devolverlo, deja nombre y zona en la sesión (el
     * Home filtra por esa zona). Mismo comportamiento que el mock.
     */
    private static Usuario perfilPropio(UsuarioResponse json) {
        Usuario usuario = json.aModelo();
        SesionUsuario.getInstancia().actualizarDesdePerfil(usuario);
        return usuario;
    }

    /**
     * Pide una página y, si el servidor dice que hay más, la siguiente, juntando
     * todo en {@code acumuladas}. Las páginas van de a una (cada pedido sale
     * recién cuando llegó el anterior), así el orden se conserva.
     */
    private void pedirPaginaDePublicaciones(String usuarioId, int pagina,
                                            List<Publicacion> acumuladas,
                                            RepositorioCallback<List<Publicacion>> callback) {
        String errorPorDefecto = "No pudimos cargar las publicaciones";
        api.obtenerPublicacionesDeVendedor(usuarioId, pagina).enqueue(
                adaptar(new RepositorioCallback<PaginaPublicacionesResponse>() {
                    @Override
                    public void onExito(PaginaPublicacionesResponse respuesta) {
                        acumuladas.addAll(activas(respuesta));
                        if (respuesta.hayMas && pagina + 1 < MAXIMO_PAGINAS_PUBLICACIONES) {
                            pedirPaginaDePublicaciones(usuarioId, pagina + 1, acumuladas, callback);
                        } else {
                            callback.onExito(acumuladas);
                        }
                    }

                    @Override
                    public void onError(String mensaje) {
                        callback.onError(mensaje);
                    }
                }, errorPorDefecto, respuesta -> respuesta));
    }

    /**
     * Las publicaciones activas de una página, ya como modelo. Se descartan las que
     * no son activas (el perfil muestra solo esas) y las que traen un valor que la
     * app no conoce.
     */
    static List<Publicacion> activas(PaginaPublicacionesResponse respuesta) {
        List<Publicacion> resultado = new ArrayList<>();
        if (respuesta.publicaciones == null) {
            return resultado;
        }
        for (PublicacionResponse json : respuesta.publicaciones) {
            Publicacion publicacion = json.aModelo();
            if (publicacion != null
                    && publicacion.getEstadoPublicacion() == EstadoPublicacion.ACTIVA) {
                resultado.add(publicacion);
            }
        }
        return resultado;
    }

    /**
     * Traduce el {@link Callback} de Retrofit al {@link RepositorioCallback} de la
     * app: 2xx con cuerpo es éxito; cualquier otro código es error con el
     * {@code detail} del servidor; {@code onFailure} es falta de conexión.
     * Retrofit ya llama a {@code onResponse}/{@code onFailure} en el Main Thread.
     */
    private static <T, R> Callback<T> adaptar(RepositorioCallback<R> callback,
                                              String errorPorDefecto,
                                              Function<T, R> aModelo) {
        return new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> llamada, @NonNull Response<T> respuesta) {
                T cuerpo = respuesta.body();
                if (!respuesta.isSuccessful() || cuerpo == null) {
                    callback.onError(ErrorApi.mensaje(respuesta, errorPorDefecto));
                    return;
                }
                R modelo;
                try {
                    modelo = aModelo.apply(cuerpo);
                } catch (RuntimeException excepcion) {
                    // JSON distinto al contrato (campo faltante, enum desconocido).
                    callback.onError(errorPorDefecto);
                    return;
                }
                callback.onExito(modelo);
            }

            @Override
            public void onFailure(@NonNull Call<T> llamada, @NonNull Throwable error) {
                callback.onError(ErrorApi.SIN_CONEXION);
            }
        };
    }
}
