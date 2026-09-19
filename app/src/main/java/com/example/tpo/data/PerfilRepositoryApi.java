package com.example.tpo.data;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.tpo.data.remote.ErrorApi;
import com.example.tpo.data.remote.UsuarioApi;
import com.example.tpo.data.remote.dto.ActualizarPerfilRequest;
import com.example.tpo.data.remote.dto.CalificacionResponse;
import com.example.tpo.data.remote.dto.UsuarioResponse;
import com.example.tpo.model.Calificacion;
import com.example.tpo.model.Usuario;
import com.example.tpo.util.ImagenUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Implementación de {@link PerfilRepository} contra la API REST, con Retrofit.
 * <p>
 * <b>Todavía no se inyecta</b>: se activa en {@code di/RepositoryModule} cuando el
 * backend de Walter esté levantado (ver {@code docs/contrato-api-perfil-historial.md}).
 * Las pantallas no cambian: reciben los mismos modelos y los mismos mensajes de
 * error por {@link RepositorioCallback} que con {@link PerfilRepositoryMock}.
 * <p>
 * No construye Retrofit: recibe {@link UsuarioApi} ya creada desde el único
 * {@code Retrofit} de {@code NetworkModule}, que agrega el token JWT.
 */
public class PerfilRepositoryApi implements PerfilRepository {

    private final UsuarioApi api;
    private final ContentResolver resolver;
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());
    /** Para comprimir y decodificar fotos fuera del Main Thread. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PerfilRepositoryApi(UsuarioApi api, Context context) {
        this.api = api;
        this.resolver = context.getApplicationContext().getContentResolver();
    }

    @Override
    public void obtenerMiPerfil(RepositorioCallback<Usuario> callback) {
        api.obtenerMiPerfil().enqueue(
                adaptar(callback, "No pudimos cargar tu perfil", PerfilRepositoryApi::perfilPropio));
    }

    @Override
    public void actualizarMiPerfil(Usuario usuario, RepositorioCallback<Usuario> callback) {
        api.actualizarMiPerfil(new ActualizarPerfilRequest(usuario)).enqueue(
                adaptar(callback, "No pudimos guardar los cambios", PerfilRepositoryApi::perfilPropio));
    }

    @Override
    public void obtenerPerfilPublico(String usuarioId, RepositorioCallback<Usuario> callback) {
        api.obtenerPerfilPublico(usuarioId).enqueue(
                adaptar(callback, "No pudimos cargar el perfil", UsuarioResponse::aModelo));
    }

    @Override
    public void actualizarFoto(Uri foto, RepositorioCallback<Usuario> callback) {
        executor.execute(() -> {
            byte[] jpeg;
            try {
                jpeg = ImagenUtils.comprimirJpeg(resolver, foto, ImagenUtils.LADO_MAXIMO_FOTO_PERFIL);
            } catch (IOException | RuntimeException excepcion) {
                handlerPrincipal.post(() -> callback.onError("No pudimos leer la imagen elegida"));
                return;
            }
            RequestBody cuerpo = RequestBody.create(jpeg, MediaType.get("image/jpeg"));
            MultipartBody.Part parte = MultipartBody.Part.createFormData("foto", "perfil.jpg", cuerpo);
            api.subirFoto(parte).enqueue(
                    adaptar(callback, "No pudimos subir la foto", UsuarioResponse::aModelo));
        });
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
