package com.example.tpo.data;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;

import com.example.tpo.model.Calificacion;
import com.example.tpo.model.Usuario;
import com.example.tpo.util.ImagenUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementación de {@link PerfilRepository} sin servidor, sobre
 * {@link BaseDeDatosMock}.
 * <p>
 * Simula todo lo que va a hacer la API: una demora de red, la identidad del que
 * pide (el servidor la saca del token; acá sale de {@link SesionUsuario}), las
 * validaciones y los mensajes de error. Así, reemplazarla por
 * {@code PerfilRepositoryApi} en {@code di/RepositoryModule} no cambia nada en
 * las pantallas: el camino de éxito y el de error son los mismos.
 * <p>
 * Las validaciones de los datos editados también se hacen acá y no en el Fragment.
 * En la app final esa responsabilidad es del backend, así que dejarla del lado del
 * repositorio es lo que hace que el reemplazo sea directo: el día que responda un
 * 400, la pantalla ya sabe qué hacer porque el camino de error es el mismo.
 */
public class PerfilRepositoryMock implements PerfilRepository {

    /** Demora artificial de la respuesta, para que se vea el estado de carga. */
    private static final long DEMORA_SIMULADA_MS = 500;

    /**
     * Poner en true para probar la pantalla de error sin backend caído.
     * Debe quedar en false en lo que se entrega.
     */
    private static final boolean SIMULAR_ERROR = false;

    private final BaseDeDatosMock base = BaseDeDatosMock.getInstancia();

    /** Para leer la foto elegida en la galería. Es el de la aplicación: no retiene ninguna pantalla. */
    private final ContentResolver resolver;

    /** Handler del Main Thread: garantiza que el callback llegue donde se puede tocar la UI. */
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    /** Un solo hilo alcanza para comprimir y decodificar fotos: nunca hay dos a la vez. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PerfilRepositoryMock(Context context) {
        this.resolver = context.getApplicationContext().getContentResolver();
    }

    // ---------------------------------------------------------------------
    // Operaciones
    // ---------------------------------------------------------------------

    @Override
    public void obtenerMiPerfil(RepositorioCallback<Usuario> callback) {
        responderDemorado(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos cargar tu perfil");
                return;
            }
            Usuario yo = base.perfilPropio(idLogueado(), System.currentTimeMillis());
            if (yo == null) {
                // Equivale a un 401: el token apunta a un usuario que ya no existe.
                callback.onError("No encontramos tu perfil");
                return;
            }
            // Deja en la sesión el nombre y la zona reales (el login solo conoce el
            // email): el Home filtra por esa zona y el Detalle firma con ese nombre.
            SesionUsuario.getInstancia().actualizarDesdePerfil(yo);
            callback.onExito(yo);
        });
    }

    @Override
    public void actualizarMiPerfil(Usuario usuario, RepositorioCallback<Usuario> callback) {
        responderDemorado(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos guardar los cambios");
                return;
            }

            String errorValidacion = validar(usuario);
            if (errorValidacion != null) {
                callback.onError(errorValidacion);
                return;
            }

            // Se normaliza igual que lo haría el backend: sin espacios sobrantes y
            // el email en minúsculas, así dos altas con distinto casing no generan
            // usuarios distintos.
            String email = usuario.getEmail().trim().toLowerCase();
            if (base.emailEnUso(email, idLogueado())) {
                // Equivale al 409 de la API.
                callback.onError("Ese email ya está en uso por otra cuenta");
                return;
            }
            base.actualizarDatosPersonales(idLogueado(),
                    usuario.getNombre().trim(),
                    email,
                    usuario.getTelefono() == null ? "" : usuario.getTelefono().trim(),
                    usuario.getZona());

            Usuario guardado = base.perfilPropio(idLogueado(), System.currentTimeMillis());

            // La sesión guarda su propia copia de nombre y zona (el Home la usa para
            // el filtro de cercanía). Si no se sincroniza acá, el usuario cambia su
            // zona en el perfil y el Home sigue filtrando por la anterior.
            SesionUsuario.getInstancia().actualizarDesdePerfil(guardado);

            callback.onExito(guardado);
        });
    }

    @Override
    public void obtenerPerfilPublico(String usuarioId, RepositorioCallback<Usuario> callback) {
        responderDemorado(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos cargar el perfil");
                return;
            }
            Usuario usuario = base.perfilPublico(usuarioId);
            if (usuario == null) {
                // Equivale al 404 de la API: el recurso no existe.
                callback.onError("Este usuario ya no está disponible");
                return;
            }
            callback.onExito(usuario);
        });
    }

    @Override
    public void actualizarFoto(Uri foto, RepositorioCallback<Usuario> callback) {
        String usuarioId = idLogueado();
        // Leer y comprimir la imagen es lento: va al executor. El resultado vuelve
        // al Main Thread, que es donde vive la base simulada y donde se llama al callback.
        executor.execute(() -> {
            byte[] jpeg;
            try {
                jpeg = ImagenUtils.comprimirJpeg(resolver, foto, ImagenUtils.LADO_MAXIMO_FOTO_PERFIL);
            } catch (IOException | RuntimeException excepcion) {
                handlerPrincipal.post(() -> callback.onError("No pudimos leer la imagen elegida"));
                return;
            }
            responderDemorado(() -> {
                if (SIMULAR_ERROR) {
                    callback.onError("No pudimos subir la foto");
                    return;
                }
                base.guardarFoto(usuarioId, jpeg);
                callback.onExito(base.perfilPropio(usuarioId, System.currentTimeMillis()));
            });
        });
    }

    @Override
    public void obtenerFoto(Usuario usuario, RepositorioCallback<Bitmap> callback) {
        byte[] bytes = base.foto(usuario.getId());
        if (bytes == null) {
            // Equivale al 404: el usuario no tiene foto.
            responderDemorado(() -> callback.onError("Este usuario no tiene foto"));
            return;
        }
        executor.execute(() -> {
            Bitmap imagen = ImagenUtils.decodificar(bytes);
            handlerPrincipal.post(() -> {
                if (imagen == null) {
                    callback.onError("No pudimos mostrar la foto");
                } else {
                    callback.onExito(imagen);
                }
            });
        });
    }

    @Override
    public void obtenerCalificacionesRecibidas(String usuarioId,
                                               RepositorioCallback<List<Calificacion>> callback) {
        responderDemorado(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos cargar las calificaciones");
                return;
            }
            if (!base.existeUsuario(usuarioId)) {
                callback.onError("Este usuario ya no está disponible");
                return;
            }
            callback.onExito(base.calificacionesRecibidas(usuarioId));
        });
    }

    // ---------------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------------

    /** Quién está pidiendo. Contra la API real lo resuelve el servidor a partir del JWT. */
    private String idLogueado() {
        return base.idParaMockOSuplente(SesionUsuario.getInstancia().getUsuarioId());
    }

    /** postDelayed simula la latencia de red y deja el callback en el Main Thread. */
    private void responderDemorado(Runnable accion) {
        handlerPrincipal.postDelayed(accion, DEMORA_SIMULADA_MS);
    }

    /**
     * Valida los datos editables. Devuelve el mensaje de error, o null si está todo bien.
     * <p>
     * Devuelve String y no lanza excepción porque el mensaje va derecho al callback
     * de error, que es el mismo camino que va a usar la API cuando responda un 400.
     */
    private String validar(Usuario usuario) {
        if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
            return "Ingresá tu nombre";
        }
        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            return "Ingresá tu email";
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(usuario.getEmail().trim()).matches()) {
            return "El email no tiene un formato válido";
        }
        if (usuario.getZona() == null) {
            return "Elegí tu zona";
        }
        // El teléfono es opcional, pero si lo cargan tiene que ser usable: es el
        // dato con el que la otra parte coordina la entrega en mano.
        String telefono = usuario.getTelefono() == null ? "" : usuario.getTelefono().trim();
        if (!telefono.isEmpty() && telefono.replaceAll("[^0-9]", "").length() < 8) {
            return "El teléfono debe tener al menos 8 dígitos";
        }
        return null;
    }
}
