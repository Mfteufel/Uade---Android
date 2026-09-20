package com.example.tpo.data;

import androidx.annotation.Nullable;

import com.example.tpo.model.Calificacion;
import com.example.tpo.model.EstadoOperacion;
import com.example.tpo.model.FiltroOperaciones;
import com.example.tpo.model.Operacion;
import com.example.tpo.model.Reputacion;
import com.example.tpo.model.TipoOperacion;
import com.example.tpo.model.Usuario;
import com.example.tpo.model.UsuarioResumen;
import com.example.tpo.model.Zona;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Backend" en memoria para los Puntos 2 y 9, mientras no exista la API de Walter.
 * <p>
 * Guarda las tres tablas que va a tener el servidor (usuarios, operaciones y
 * calificaciones) y hace lo que va a hacer el servidor con ellas: calcular la
 * reputación y validar las reglas para calificar. {@link PerfilRepositoryMock} y
 * {@link OperacionRepositoryMock} la comparten; por eso calificar a alguien desde
 * el historial cambia la reputación que se ve en su perfil público, sin que un
 * mock tenga que llamar al otro.
 * <p>
 * Es Java puro (sin Handler ni Context) para poder probarla con JUnit. Los
 * repositorios mock se encargan de la demora simulada y del Main Thread, y la
 * usan siempre desde el Main Thread, así que no necesita sincronización.
 * <p>
 * El día que se conecte la API, esta clase y los mocks simplemente dejan de
 * inyectarse (ver {@code di/RepositoryModule}); ninguna pantalla la conoce.
 */
public class BaseDeDatosMock {

    /** Id del usuario logueado mientras el login no traiga el id real del backend. */
    public static final String ID_USUARIO_DEMO = "u0";

    /** Ventana para calificar después de la entrega. La misma regla valida el servidor. */
    public static final int DIAS_PARA_CALIFICAR = 7;

    static final long MILIS_POR_DIA = 24L * 60L * 60L * 1000L;

    private static BaseDeDatosMock instancia;

    private final Map<String, FilaUsuario> usuarios = new LinkedHashMap<>();
    private final List<FilaOperacion> operaciones = new ArrayList<>();
    private final List<Calificacion> calificaciones = new ArrayList<>();
    /** Fotos de perfil ya comprimidas, como las guardaría el servidor en disco. */
    private final Map<String, byte[]> fotos = new HashMap<>();

    private int proximoIdOperacion = 1;
    private int proximoIdCalificacion = 1;

    /** Base vacía. Pensada para los tests; la app usa {@link #getInstancia()}. */
    public BaseDeDatosMock() {
    }

    public static synchronized BaseDeDatosMock getInstancia() {
        if (instancia == null) {
            instancia = conDatosDePrueba(System.currentTimeMillis());
        }
        return instancia;
    }

    // ---------------------------------------------------------------------
    // Usuarios (Punto 2)
    // ---------------------------------------------------------------------

    public void agregarUsuario(String id, String nombre, String email, String telefono,
                               @Nullable Zona zona, long fechaAlta) {
        usuarios.put(id, new FilaUsuario(id, nombre, email, telefono, zona, fechaAlta));
    }

    public boolean existeUsuario(String usuarioId) {
        return usuarios.containsKey(usuarioId);
    }

    /**
     * El id que hay que usar contra este catálogo demo: el de la sesión, si existe
     * acá, o si no el usuario demo. Hace falta porque el login (Punto 1) ya habla
     * con el backend real y deja en {@code SesionUsuario} un id numérico que este
     * catálogo fijo no conoce — sin este fallback, Perfil e Historial (que siguen
     * en mock hasta que el backend tenga esos endpoints) mostrarían "no
     * encontramos tu perfil" para cualquiera que entre con un login real.
     */
    public String idParaMockOSuplente(String idDeSesion) {
        return existeUsuario(idDeSesion) ? idDeSesion : ID_USUARIO_DEMO;
    }

    /** Equivale a {@code GET /usuarios/me}: todos los datos, incluidos email y teléfono. */
    @Nullable
    public Usuario perfilPropio(String usuarioId, long ahora) {
        FilaUsuario fila = usuarios.get(usuarioId);
        if (fila == null) {
            return null;
        }
        return new Usuario(fila.id, fila.nombre, fila.email, fila.telefono, fila.zona,
                fila.fechaAlta, calcularReputacion(usuarioId), urlFoto(usuarioId),
                contarCalificacionesPendientes(usuarioId, ahora));
    }

    /** Equivale a {@code GET /usuarios/{id}}: sin email ni teléfono, que son privados. */
    @Nullable
    public Usuario perfilPublico(String usuarioId) {
        FilaUsuario fila = usuarios.get(usuarioId);
        if (fila == null) {
            return null;
        }
        return new Usuario(fila.id, fila.nombre, null, null, fila.zona, fila.fechaAlta,
                calcularReputacion(usuarioId), urlFoto(usuarioId), 0);
    }

    /** true si otro usuario ya usa ese email (el servidor respondería 409). */
    public boolean emailEnUso(String email, String exceptoUsuarioId) {
        for (FilaUsuario fila : usuarios.values()) {
            if (!fila.id.equals(exceptoUsuarioId) && fila.email.equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }

    /** Equivale a {@code PATCH /usuarios/me}. Los datos llegan ya validados y normalizados. */
    public void actualizarDatosPersonales(String usuarioId, String nombre, String email,
                                          String telefono, Zona zona) {
        FilaUsuario fila = usuarios.get(usuarioId);
        if (fila == null) {
            return;
        }
        fila.nombre = nombre;
        fila.email = email;
        fila.telefono = telefono;
        fila.zona = zona;
    }

    /** Equivale a {@code PUT /usuarios/me/foto}: reemplaza la foto anterior si había. */
    public void guardarFoto(String usuarioId, byte[] jpeg) {
        fotos.put(usuarioId, jpeg);
    }

    /** Equivale a {@code GET /usuarios/{id}/foto}. {@code null} si no tiene (el servidor daría 404). */
    @Nullable
    public byte[] foto(String usuarioId) {
        return fotos.get(usuarioId);
    }

    @Nullable
    private String urlFoto(String usuarioId) {
        return fotos.containsKey(usuarioId) ? "usuarios/" + usuarioId + "/foto" : null;
    }

    // ---------------------------------------------------------------------
    // Reputación (Punto 2 alimentado por el Punto 9)
    // ---------------------------------------------------------------------

    /**
     * Calcula la reputación a partir de las filas, igual que las cuatro consultas
     * agregadas del servidor: promedio y cantidad de calificaciones recibidas, y
     * operaciones <em>entregadas</em> como comprador y como vendedor. No se guarda
     * en ningún lado: así nunca puede quedar desincronizada.
     */
    public Reputacion calcularReputacion(String usuarioId) {
        int sumaEstrellas = 0;
        int cantidadCalificaciones = 0;
        for (Calificacion calificacion : calificaciones) {
            if (calificacion.getCalificadoId().equals(usuarioId)) {
                sumaEstrellas += calificacion.getEstrellas();
                cantidadCalificaciones++;
            }
        }
        int compras = 0;
        int ventas = 0;
        for (FilaOperacion fila : operaciones) {
            if (fila.estado != EstadoOperacion.ENTREGADA) {
                continue;
            }
            if (fila.compradorId.equals(usuarioId)) {
                compras++;
            } else if (fila.vendedorId.equals(usuarioId)) {
                ventas++;
            }
        }
        double promedio = cantidadCalificaciones == 0
                ? 0 : (double) sumaEstrellas / cantidadCalificaciones;
        return new Reputacion(promedio, cantidadCalificaciones, compras, ventas);
    }

    /** Equivale a {@code GET /usuarios/{id}/calificaciones}: las recibidas, más recientes primero. */
    public List<Calificacion> calificacionesRecibidas(String usuarioId) {
        List<Calificacion> resultado = new ArrayList<>();
        for (Calificacion calificacion : calificaciones) {
            if (calificacion.getCalificadoId().equals(usuarioId)) {
                resultado.add(calificacion);
            }
        }
        Collections.sort(resultado, (a, b) -> Long.compare(b.getFecha(), a.getFecha()));
        return resultado;
    }

    // ---------------------------------------------------------------------
    // Operaciones (Punto 9)
    // ---------------------------------------------------------------------

    /**
     * Crea una operación pendiente de entrega. En el backend real la dispara el
     * Punto 7 al aceptar una oferta, en la misma transacción (ver
     * {@code docs/contrato-api-perfil-historial.md}).
     *
     * @return el id de la operación creada.
     * @throws IllegalArgumentException si comprador y vendedor son la misma persona
     *                                  (el servidor lo impide con un CHECK).
     */
    public String crearOperacion(@Nullable String publicacionId, String tituloArticulo,
                                 String compradorId, String vendedorId,
                                 double montoFinal, long fechaOperacion) {
        if (compradorId.equals(vendedorId)) {
            throw new IllegalArgumentException("Nadie puede comprarse a sí mismo");
        }
        String id = "op" + proximoIdOperacion++;
        operaciones.add(new FilaOperacion(id, publicacionId, tituloArticulo, compradorId,
                vendedorId, montoFinal, fechaOperacion));
        return id;
    }

    /** Confirma la entrega (Punto 8). Desde este momento corre la ventana para calificar. */
    public void registrarEntrega(String operacionId, long fechaEntrega) {
        FilaOperacion fila = buscarOperacion(operacionId);
        if (fila != null && fila.estado == EstadoOperacion.PENDIENTE_ENTREGA) {
            fila.estado = EstadoOperacion.ENTREGADA;
            fila.fechaEntrega = fechaEntrega;
        }
    }

    /**
     * Equivale a {@code GET /operaciones?tipo=&desde=&hasta=}: las operaciones
     * concretadas (entregadas) del usuario, vistas desde su lado, más recientes
     * primero. Nunca devuelve operaciones en las que no participó.
     */
    public List<Operacion> historial(String usuarioId, FiltroOperaciones filtro, long ahora) {
        List<Operacion> resultado = new ArrayList<>();
        for (FilaOperacion fila : operaciones) {
            if (fila.estado != EstadoOperacion.ENTREGADA || !participa(fila, usuarioId)) {
                continue;
            }
            Operacion operacion = verDesde(fila, usuarioId, ahora);
            if (filtro.incluye(operacion)) {
                resultado.add(operacion);
            }
        }
        Collections.sort(resultado, (a, b) ->
                Long.compare(b.getFechaReferencia(), a.getFechaReferencia()));
        return resultado;
    }

    /** Cuántas operaciones propias se pueden calificar todavía (aviso del perfil propio). */
    public int contarCalificacionesPendientes(String usuarioId, long ahora) {
        int pendientes = 0;
        for (FilaOperacion fila : operaciones) {
            if (participa(fila, usuarioId) && puedeCalificar(fila, usuarioId, ahora)) {
                pendientes++;
            }
        }
        return pendientes;
    }

    // ---------------------------------------------------------------------
    // Calificaciones (Punto 9)
    // ---------------------------------------------------------------------

    /**
     * Valida una calificación con las mismas reglas que el servidor. Devuelve el
     * mensaje de error (el {@code detail} que mandaría la API) o {@code null} si
     * se puede calificar.
     * <p>
     * No recibe a quién se califica: el calificado es siempre la otra parte de la
     * operación. Por eso nadie puede calificarse a sí mismo ni calificar a un
     * tercero, aunque la app mandara datos adulterados.
     */
    @Nullable
    public String validarCalificacion(String operacionId, String autorId, int estrellas,
                                      @Nullable String comentario, long ahora) {
        FilaOperacion fila = buscarOperacion(operacionId);
        if (fila == null) {
            return "La operación no existe";
        }
        if (!participa(fila, autorId)) {
            return "No participaste de esta operación";
        }
        if (estrellas < Calificacion.ESTRELLAS_MINIMAS || estrellas > Calificacion.ESTRELLAS_MAXIMAS) {
            return "La calificación tiene que ser de 1 a 5 estrellas";
        }
        if (comentario != null && comentario.trim().length() > Calificacion.LARGO_MAXIMO_COMENTARIO) {
            return "El comentario puede tener hasta "
                    + Calificacion.LARGO_MAXIMO_COMENTARIO + " caracteres";
        }
        if (fila.estado != EstadoOperacion.ENTREGADA || fila.fechaEntrega == null) {
            return "Todavía no se registró la entrega";
        }
        if (buscarCalificacion(operacionId, autorId) != null) {
            return "Ya calificaste esta operación";
        }
        if (ahora > finVentana(fila)) {
            return "Venció el plazo para calificar (" + DIAS_PARA_CALIFICAR
                    + " días desde la entrega)";
        }
        return null;
    }

    /**
     * Equivale a {@code POST /operaciones/{id}/calificacion}. Llamar solo después
     * de {@link #validarCalificacion} sin errores.
     *
     * @return la operación actualizada, vista desde el autor (ya con su calificación).
     */
    public Operacion calificar(String operacionId, String autorId, int estrellas,
                               @Nullable String comentario, long ahora) {
        FilaOperacion fila = buscarOperacion(operacionId);
        if (fila == null) {
            throw new IllegalArgumentException("La operación no existe: " + operacionId);
        }
        String calificadoId = fila.compradorId.equals(autorId) ? fila.vendedorId : fila.compradorId;
        String comentarioLimpio = comentario == null || comentario.trim().isEmpty()
                ? null : comentario.trim();
        calificaciones.add(new Calificacion("c" + proximoIdCalificacion++, operacionId,
                resumen(autorId), calificadoId, fila.tituloArticulo, estrellas,
                comentarioLimpio, ahora));
        return verDesde(fila, autorId, ahora);
    }

    // ---------------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------------

    @Nullable
    private FilaOperacion buscarOperacion(String operacionId) {
        for (FilaOperacion fila : operaciones) {
            if (fila.id.equals(operacionId)) {
                return fila;
            }
        }
        return null;
    }

    @Nullable
    private Calificacion buscarCalificacion(String operacionId, String autorId) {
        for (Calificacion calificacion : calificaciones) {
            if (calificacion.getOperacionId().equals(operacionId)
                    && calificacion.getAutor().getId().equals(autorId)) {
                return calificacion;
            }
        }
        return null;
    }

    private static boolean participa(FilaOperacion fila, String usuarioId) {
        return fila.compradorId.equals(usuarioId) || fila.vendedorId.equals(usuarioId);
    }

    private static long finVentana(FilaOperacion fila) {
        return fila.fechaEntrega + DIAS_PARA_CALIFICAR * MILIS_POR_DIA;
    }

    private boolean puedeCalificar(FilaOperacion fila, String usuarioId, long ahora) {
        return fila.estado == EstadoOperacion.ENTREGADA
                && fila.fechaEntrega != null
                && ahora <= finVentana(fila)
                && buscarCalificacion(fila.id, usuarioId) == null;
    }

    /** Arma la respuesta de una operación como la vería {@code usuarioId}. */
    private Operacion verDesde(FilaOperacion fila, String usuarioId, long ahora) {
        TipoOperacion tipo = fila.compradorId.equals(usuarioId)
                ? TipoOperacion.COMPRA : TipoOperacion.VENTA;
        Long calificableHasta = fila.fechaEntrega == null ? null : finVentana(fila);
        return new Operacion(fila.id, fila.publicacionId, fila.tituloArticulo, fila.montoFinal,
                fila.fechaOperacion, fila.fechaEntrega, fila.estado,
                resumen(fila.compradorId), resumen(fila.vendedorId), tipo,
                buscarCalificacion(fila.id, usuarioId),
                puedeCalificar(fila, usuarioId, ahora), calificableHasta);
    }

    private UsuarioResumen resumen(String usuarioId) {
        FilaUsuario fila = usuarios.get(usuarioId);
        return new UsuarioResumen(usuarioId, fila == null ? "Usuario" : fila.nombre);
    }

    /** Fila de la tabla usuarios. Mutable porque el perfil se edita. */
    private static class FilaUsuario {
        final String id;
        String nombre;
        String email;
        String telefono;
        @Nullable
        Zona zona;
        final long fechaAlta;

        FilaUsuario(String id, String nombre, String email, String telefono,
                    @Nullable Zona zona, long fechaAlta) {
            this.id = id;
            this.nombre = nombre;
            this.email = email;
            this.telefono = telefono;
            this.zona = zona;
            this.fechaAlta = fechaAlta;
        }
    }

    /** Fila de la tabla operaciones: guarda ids, no la vista de cada usuario. */
    private static class FilaOperacion {
        final String id;
        @Nullable
        final String publicacionId;
        final String tituloArticulo;
        final String compradorId;
        final String vendedorId;
        final double montoFinal;
        final long fechaOperacion;
        EstadoOperacion estado = EstadoOperacion.PENDIENTE_ENTREGA;
        @Nullable
        Long fechaEntrega;

        FilaOperacion(String id, @Nullable String publicacionId, String tituloArticulo,
                      String compradorId, String vendedorId, double montoFinal,
                      long fechaOperacion) {
            this.id = id;
            this.publicacionId = publicacionId;
            this.tituloArticulo = tituloArticulo;
            this.compradorId = compradorId;
            this.vendedorId = vendedorId;
            this.montoFinal = montoFinal;
            this.fechaOperacion = fechaOperacion;
        }
    }

    // ---------------------------------------------------------------------
    // Datos de prueba
    // ---------------------------------------------------------------------

    /**
     * Base con datos de prueba relativos a {@code ahora}.
     * <p>
     * Los vendedores usan los mismos ids y nombres que el catálogo de
     * {@link PublicacionRepositoryMock} ("v1".."v12"): así, desde el Detalle, "ver
     * perfil" abre a la persona correcta. El usuario logueado ("u0") coincide con
     * el usuario demo del backend ({@code walter@uade.edu.ar}).
     * <p>
     * Las operaciones del usuario logueado cubren a propósito todos los casos de la
     * regla de 7 días: calificables, una que vence mañana, una vencida sin
     * calificar, ya calificadas y una pendiente de entrega (que no aparece en el
     * historial porque todavía no se concretó).
     */
    public static BaseDeDatosMock conDatosDePrueba(long ahora) {
        BaseDeDatosMock base = new BaseDeDatosMock();

        base.agregarUsuario(ID_USUARIO_DEMO, "Walter", "walter@uade.edu.ar", "1145678901",
                Zona.CABALLITO, ahora - 420 * MILIS_POR_DIA);
        base.agregarUsuario("v1", "Martina G.", "martina.g@ronda.com", "1156781234", Zona.PALERMO, haceMeses(ahora, 20));
        base.agregarUsuario("v2", "Nicolás P.", "nicolas.p@ronda.com", "1134567890", Zona.CABALLITO, haceMeses(ahora, 9));
        base.agregarUsuario("v3", "Sofía M.", "sofia.m@ronda.com", "", Zona.ALMAGRO, haceMeses(ahora, 4));
        base.agregarUsuario("v4", "Familia Ruiz", "ruiz@ronda.com", "1122334455", Zona.VILLA_CRESPO, haceMeses(ahora, 30));
        base.agregarUsuario("v5", "Valeria S.", "valeria.s@ronda.com", "1166778899", Zona.SAN_ISIDRO, haceMeses(ahora, 26));
        base.agregarUsuario("v6", "Escuela de Música", "escuela@ronda.com", "1143218765", Zona.PALERMO, haceMeses(ahora, 40));
        base.agregarUsuario("v7", "Club San Martín", "club.sm@ronda.com", "1148887766", Zona.TIGRE, haceMeses(ahora, 15));
        base.agregarUsuario("v8", "Gonzalo H.", "gonzalo.h@ronda.com", "1177001122", Zona.BOEDO, haceMeses(ahora, 11));
        base.agregarUsuario("v9", "Bruno T.", "bruno.t@ronda.com", "1198765432", Zona.NUNEZ, haceMeses(ahora, 2));
        base.agregarUsuario("v10", "Rocío N.", "rocio.n@ronda.com", "1133445566", Zona.BARRACAS, haceMeses(ahora, 22));
        base.agregarUsuario("v11", "Laura y Seba", "laurayseba@ronda.com", "1155443322", Zona.VICENTE_LOPEZ, haceMeses(ahora, 18));
        base.agregarUsuario("v12", "Hernán W.", "hernan.w@ronda.com", "1199887766", Zona.QUILMES, haceMeses(ahora, 33));

        // --- Compras del usuario logueado ---
        // Entregada hace 2 días: se puede calificar (le quedan 5).
        base.operacionDePrueba(ahora, "4", "Teclado mecánico Redragon", ID_USUARIO_DEMO, "v3", 45000, 4, 2);
        // Entregada hace 6 días: se puede calificar, vence mañana.
        base.operacionDePrueba(ahora, "9", "Campera de cuero negra", ID_USUARIO_DEMO, "v1", 78000, 9, 6);
        // Entregada hace 12 días sin calificar: plazo vencido.
        base.operacionDePrueba(ahora, "17", "Rayuela - Julio Cortázar", ID_USUARIO_DEMO, "v9", 11000, 15, 12);
        String mancuernas = base.operacionDePrueba(ahora, "13", "Set de mancuernas 20kg", ID_USUARIO_DEMO, "v8", 70000, 45, 40);
        base.calificacionDePrueba(ahora, mancuernas, ID_USUARIO_DEMO, 5, "Impecable, todo como en la foto", 39);
        base.calificacionDePrueba(ahora, mancuernas, "v8", 5, "Pagó en el momento, muy buena onda", 38);
        String guitarra = base.operacionDePrueba(ahora, "19", "Guitarra criolla Fonseca", ID_USUARIO_DEMO, "v6", 125000, 120, 118);
        base.calificacionDePrueba(ahora, guitarra, ID_USUARIO_DEMO, 4, null, 117);
        base.calificacionDePrueba(ahora, guitarra, "v6", 4, "Todo bien, llegó un poco tarde al encuentro", 116);

        // --- Ventas del usuario logueado ---
        // Entregada hace 3 días: el comprador ya calificó, falta que califique el usuario.
        String bici = base.operacionDePrueba(ahora, null, "Bicicleta rodado 26", "v2", ID_USUARIO_DEMO, 85000, 5, 3);
        base.calificacionDePrueba(ahora, bici, "v2", 5, "Excelente vendedor, súper puntual", 2);
        String auriculares = base.operacionDePrueba(ahora, null, "Auriculares Sony WH-1000XM4", "v10", ID_USUARIO_DEMO, 150000, 70, 66);
        base.calificacionDePrueba(ahora, auriculares, "v10", 4, "Bien, aunque tenían un detalle que no estaba en la descripción", 65);
        base.calificacionDePrueba(ahora, auriculares, ID_USUARIO_DEMO, 5, "Compradora muy amable", 64);
        String silla = base.operacionDePrueba(ahora, null, "Silla de escritorio ergonómica", "v12", ID_USUARIO_DEMO, 60000, 200, 198);
        base.calificacionDePrueba(ahora, silla, "v12", 3, "Tardó en responder los mensajes", 196);
        // Oferta aceptada ayer, todavía sin entregar: no cuenta ni aparece en el historial.
        base.crearOperacion(null, "Lámpara de pie vintage", "v5", ID_USUARIO_DEMO, 32000, ahora - MILIS_POR_DIA);

        // --- Operaciones entre otros usuarios (para sus perfiles públicos) ---
        String iphone = base.operacionDePrueba(ahora, null, "iPhone 12 64GB", "v4", "v1", 450000, 35, 33);
        base.calificacionDePrueba(ahora, iphone, "v4", 5, "Todo perfecto, muy recomendable", 32);
        String mochila = base.operacionDePrueba(ahora, null, "Mochila Jansport", "v11", "v1", 40000, 80, 78);
        base.calificacionDePrueba(ahora, mochila, "v11", 4, "Buena comunicación", 77);
        base.calificacionDePrueba(ahora, mochila, "v1", 5, null, 77);
        String mouse = base.operacionDePrueba(ahora, null, "Mouse Logitech MX Master", "v7", "v2", 90000, 25, 24);
        base.calificacionDePrueba(ahora, mouse, "v7", 4, null, 23);
        String cafetera = base.operacionDePrueba(ahora, null, "Cafetera italiana", "v3", "v5", 30000, 60, 58);
        base.calificacionDePrueba(ahora, cafetera, "v3", 5, "Hermosa, la recomiendo", 57);
        base.calificacionDePrueba(ahora, cafetera, "v5", 5, "Compradora de diez", 57);
        String metronomo = base.operacionDePrueba(ahora, null, "Metrónomo digital", "v8", "v6", 25000, 100, 97);
        base.calificacionDePrueba(ahora, metronomo, "v8", 5, "Muy profesionales", 96);

        return base;
    }

    /** Operación ya entregada, con fechas expresadas en "hace N días". */
    private String operacionDePrueba(long ahora, @Nullable String publicacionId, String titulo,
                                     String compradorId, String vendedorId, double monto,
                                     int diasDesdeAcuerdo, int diasDesdeEntrega) {
        String id = crearOperacion(publicacionId, titulo, compradorId, vendedorId, monto,
                ahora - diasDesdeAcuerdo * MILIS_POR_DIA);
        registrarEntrega(id, ahora - diasDesdeEntrega * MILIS_POR_DIA);
        return id;
    }

    /** Calificación de prueba cargada "hace N días" (dentro de su ventana de 7 días). */
    private void calificacionDePrueba(long ahora, String operacionId, String autorId,
                                      int estrellas, @Nullable String comentario, int haceDias) {
        long fecha = ahora - haceDias * MILIS_POR_DIA;
        String error = validarCalificacion(operacionId, autorId, estrellas, comentario, fecha);
        if (error != null) {
            throw new IllegalStateException("Dato de prueba inválido: " + error);
        }
        calificar(operacionId, autorId, estrellas, comentario, fecha);
    }

    private static long haceMeses(long ahora, int meses) {
        return ahora - meses * 30L * MILIS_POR_DIA;
    }
}
