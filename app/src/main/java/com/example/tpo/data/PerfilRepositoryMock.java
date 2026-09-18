package com.example.tpo.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;

import com.example.tpo.model.Reputacion;
import com.example.tpo.model.Usuario;
import com.example.tpo.model.Zona;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementación de {@link PerfilRepository} con datos fijos en memoria.
 * <p>
 * Misma simplificación acordada que en {@link PublicacionRepositoryMock}: la
 * API_Rest del TPO todavía no existe, así que los perfiles viven acá.
 * <p>
 * <b>Cómo se reemplaza por la API de verdad:</b> se crea un
 * {@code PerfilRepositoryApi} que implemente la misma interfaz y que adentro haga
 * {@code call.enqueue(...)} con Retrofit, validando {@code response.isSuccessful()}
 * antes de leer el body. Las pantallas no se modifican.
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

    /** Id del usuario logueado mientras el Punto 1 (Autenticación) no exista. */
    public static final String ID_USUARIO_DEMO = "u0";

    private static PerfilRepositoryMock instancia;

    /**
     * Perfiles indexados por id. Es un LinkedHashMap para que el orden de alta se
     * mantenga estable entre consultas, igual que las fechas del catálogo de
     * publicaciones.
     */
    private final Map<String, Usuario> perfiles;

    /** Handler del Main Thread: garantiza que el callback llegue donde se puede tocar la UI. */
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    private PerfilRepositoryMock() {
        perfiles = crearPerfilesDePrueba();
    }

    public static synchronized PerfilRepositoryMock getInstancia() {
        if (instancia == null) {
            instancia = new PerfilRepositoryMock();
        }
        return instancia;
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
            Usuario yo = perfiles.get(SesionUsuario.getInstancia().getUsuarioId());
            if (yo == null) {
                // No debería pasar con la sesión demo, pero si el Punto 1 setea un
                // id que no existe conviene un mensaje claro y no un crash.
                callback.onError("No encontramos tu perfil");
                return;
            }
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
            Usuario guardado = usuario.conDatosPersonales(
                    usuario.getNombre().trim(),
                    usuario.getEmail().trim().toLowerCase(),
                    usuario.getTelefono().trim(),
                    usuario.getZona());

            perfiles.put(guardado.getId(), guardado);

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
            Usuario usuario = perfiles.get(usuarioId);
            if (usuario == null) {
                // Equivale al 404 de la API: el recurso no existe.
                callback.onError("Este usuario ya no está disponible");
                return;
            }
            callback.onExito(usuario);
        });
    }

    // ---------------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------------

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

    // ---------------------------------------------------------------------
    // Datos de prueba
    // ---------------------------------------------------------------------

    /** Milisegundos correspondientes a "hace N días". */
    private static long haceDias(int dias) {
        return System.currentTimeMillis() - dias * 24L * 60L * 60L * 1000L;
    }

    /**
     * Perfiles de prueba.
     * <p>
     * Los nombres coinciden con los vendedores del catálogo de
     * {@link PublicacionRepositoryMock} para que, cuando el Punto 4 (Detalle)
     * enlace la publicación con su vendedor, el perfil público muestre a la
     * persona correcta y no a un usuario inventado.
     * <p>
     * Hay variedad a propósito: usuarios con mucha reputación, con poca, y uno
     * recién registrado sin calificaciones, para poder ver los tres casos en pantalla.
     */
    private static Map<String, Usuario> crearPerfilesDePrueba() {
        Map<String, Usuario> mapa = new LinkedHashMap<>();

        // Usuario logueado.
        mapa.put(ID_USUARIO_DEMO, new Usuario(ID_USUARIO_DEMO,
                "Juan Elliff", "juan.elliff@ronda.com", "1145678901",
                Zona.CABALLITO, haceDias(420),
                new Reputacion(4.6, 12, 7)));

        mapa.put("u1", new Usuario("u1",
                "Martina G.", "martina.g@ronda.com", "1156781234",
                Zona.PALERMO, haceDias(730),
                new Reputacion(4.9, 8, 31)));

        mapa.put("u2", new Usuario("u2",
                "Nicolás P.", "nicolas.p@ronda.com", "1134567890",
                Zona.CABALLITO, haceDias(210),
                new Reputacion(4.2, 5, 9)));

        mapa.put("u3", new Usuario("u3",
                "Luciano R.", "luciano.r@ronda.com", "1167894321",
                Zona.BELGRANO, haceDias(95),
                new Reputacion(3.8, 2, 4)));

        mapa.put("u4", new Usuario("u4",
                "Sofía M.", "sofia.m@ronda.com", "",
                Zona.ALMAGRO, haceDias(1100),
                new Reputacion(5.0, 24, 18)));

        // Recién registrado: sin operaciones ni calificaciones todavía.
        mapa.put("u5", new Usuario("u5",
                "Bruno T.", "bruno.t@ronda.com", "1198765432",
                Zona.NUNEZ, haceDias(3),
                new Reputacion(0, 0, 0)));

        return mapa;
    }
}