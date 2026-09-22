package com.example.tpo.ui.detalle;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.tpo.R;
import com.example.tpo.data.FavoritoRepository;
import com.example.tpo.data.FavoritoRepositoryApi;
import com.example.tpo.data.OfertasRepository;
import com.example.tpo.data.PreguntaRepository;
import com.example.tpo.data.PreguntaRepositoryApi;
import com.example.tpo.data.PublicacionRepository;
import com.example.tpo.data.PublicacionRepositoryApi;
import com.example.tpo.data.PublicacionesVistas;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.data.SesionUsuario;
import com.example.tpo.model.EstadoOferta;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.OfertaNegociacion;
import com.example.tpo.model.Pregunta;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Vendedor;
import com.example.tpo.ui.VendedorUi;
import com.example.tpo.ui.perfil.PerfilVendedorFragment;
import com.example.tpo.util.ConectividadUtils;
import com.example.tpo.util.FormatoUtils;
import com.example.tpo.util.MapaUtils;
import com.example.tpo.util.TextoUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Detalle de publicación — Punto 4 del TPO.
 * <p>
 * Recibe el id de la publicación por argumento de navegación (nunca el objeto
 * completo) y lo resuelve contra el repositorio, igual que hace el Home: así el
 * día que exista la API_Rest el cambio es solo la implementación del
 * repositorio, no esta pantalla.
 * <p>
 * Muestra galería (lista simple de placeholders, con contador de foto actual),
 * datos de la publicación, fecha de publicación, tarjeta del vendedor
 * (reputación + acceso al perfil público) y las acciones que cambian según
 * quién mira: un interesado puede preguntar, ofertar y guardar la publicación
 * (si sigue activa); el propio vendedor accede a la gestión de su publicación
 * (pausar, reactivar, marcar vendida, ver preguntas y ofertas recibidas). El
 * rol se decide comparando el id del vendedor contra el del usuario logueado
 * (no el nombre, que puede repetirse entre personas distintas).
 */
@AndroidEntryPoint
public class DetalleFragment extends Fragment {

    private static final String ARG_PUBLICACION_ID = "publicacionId";

    /** Mínimo de caracteres para que una pregunta tenga algo de contexto. */
    private static final int LARGO_MINIMO_PREGUNTA = 10;
    /** La oferta tiene que llegar al menos a esta fracción del precio publicado. */
    private static final double PROPORCION_MINIMA_OFERTA = 0.5;

    private PublicacionRepository repositorio;
    private FavoritoRepository favoritoRepositorio;

    /** Lo inyecta Hilt: manda y lee ofertas contra el backend real (Punto 7). */
    @Inject
    OfertasRepository ofertasRepository;

    private String publicacionId;

    private PreguntaRepository preguntaRepository;
    private PublicacionesVistas publicacionesVistas;

    /** Última publicación cargada. La usan el ítem de menú y los diálogos, que viven fuera del callback. */
    @Nullable
    private Publicacion publicacionCargada;

    /** Cuántas fotos tiene la galería actual, para el contador ("2 de 3") y sus límites. */
    private int cantidadFotosGaleria = 1;

    // --- Vistas. Son null fuera del rango onCreateView..onDestroyView ---
    private MaterialToolbar toolbar;
    private NestedScrollView scrollContenido;
    private HorizontalScrollView scrollFotos;
    private LinearLayout grupoFotos;
    private TextView contadorFotos;
    private TextView tituloDetalle;
    private TextView precioDetalle;
    private TextView estadoPublicacionDetalle;
    private TextView estadoDetalle;
    private TextView categoriaDetalle;
    private TextView zonaYFechaDetalle;
    private TextView fechaPublicacionDetalle;
    private TextView descripcionDetalle;
    private View bloqueMisInteracciones;
    private LinearLayout grupoMisInteracciones;
    private View bloquePuntoEntrega;
    private TextView textoEntregaBloqueada;
    private TextView direccionEntregaDetalle;
    private MaterialButton botonComoLlegar;
    private View tarjetaVendedor;
    private TextView inicialesVendedor;
    private TextView nombreVendedorDetalle;
    private TextView reputacionVendedorDetalle;
    private TextView miembroDesdeVendedorDetalle;
    private MaterialButton botonVerPerfil;
    private CircularProgressIndicator progresoInicial;
    private View estadoError;
    private TextView textoError;
    private View barraAcciones;
    private TextView textoRolAviso;
    private View accionesInteresado;
    private MaterialButton botonPreguntar;
    private MaterialButton botonOfertar;
    private MaterialButton botonGestionar;

    /** Ítem "Guardar" de la toolbar. Es una referencia a vista: también se limpia. */
    @Nullable
    private MenuItem itemGuardar;

    /** Diálogo abierto, si hay. Se cierra en onDestroyView para no filtrar la Activity. */
    @Nullable
    private AlertDialog dialogoActivo;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        preguntaRepository = PreguntaRepositoryApi.getInstancia(context);
        publicacionesVistas = PublicacionesVistas.getInstancia(context);
        repositorio = PublicacionRepositoryApi.getInstancia(context);
        favoritoRepositorio = FavoritoRepositoryApi.getInstancia(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        publicacionId = requireArguments().getString(ARG_PUBLICACION_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detalle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.toolbar);
        scrollContenido = view.findViewById(R.id.scrollContenido);
        scrollFotos = view.findViewById(R.id.scrollFotos);
        grupoFotos = view.findViewById(R.id.grupoFotos);
        contadorFotos = view.findViewById(R.id.contadorFotos);
        tituloDetalle = view.findViewById(R.id.tituloDetalle);
        precioDetalle = view.findViewById(R.id.precioDetalle);
        estadoPublicacionDetalle = view.findViewById(R.id.estadoPublicacionDetalle);
        estadoDetalle = view.findViewById(R.id.estadoDetalle);
        categoriaDetalle = view.findViewById(R.id.categoriaDetalle);
        zonaYFechaDetalle = view.findViewById(R.id.zonaYFechaDetalle);
        fechaPublicacionDetalle = view.findViewById(R.id.fechaPublicacionDetalle);
        descripcionDetalle = view.findViewById(R.id.descripcionDetalle);
        bloqueMisInteracciones = view.findViewById(R.id.bloqueMisInteracciones);
        grupoMisInteracciones = view.findViewById(R.id.grupoMisInteracciones);
        bloquePuntoEntrega = view.findViewById(R.id.bloquePuntoEntrega);
        textoEntregaBloqueada = view.findViewById(R.id.textoEntregaBloqueada);
        direccionEntregaDetalle = view.findViewById(R.id.direccionEntregaDetalle);
        botonComoLlegar = view.findViewById(R.id.botonComoLlegar);
        tarjetaVendedor = view.findViewById(R.id.tarjetaVendedor);
        inicialesVendedor = view.findViewById(R.id.inicialesVendedor);
        nombreVendedorDetalle = view.findViewById(R.id.nombreVendedorDetalle);
        reputacionVendedorDetalle = view.findViewById(R.id.reputacionVendedorDetalle);
        miembroDesdeVendedorDetalle = view.findViewById(R.id.miembroDesdeVendedorDetalle);
        botonVerPerfil = view.findViewById(R.id.botonVerPerfil);
        progresoInicial = view.findViewById(R.id.progresoInicial);
        estadoError = view.findViewById(R.id.estadoError);
        textoError = view.findViewById(R.id.textoError);
        barraAcciones = view.findViewById(R.id.barraAcciones);
        textoRolAviso = view.findViewById(R.id.textoRolAviso);
        accionesInteresado = view.findViewById(R.id.accionesInteresado);
        botonPreguntar = view.findViewById(R.id.botonPreguntar);
        botonOfertar = view.findViewById(R.id.botonOfertar);
        botonGestionar = view.findViewById(R.id.botonGestionar);

        configurarToolbar(view);
        configurarGaleria();
        escucharResultadoDeGestion();
        view.findViewById(R.id.botonReintentar).setOnClickListener(v -> cargarPublicacion());

        cargarPublicacion();
    }

    /** Recalcula el contador de fotos ("2 de 3") a medida que el usuario scrollea la tira. */
    private void configurarGaleria() {
        scrollFotos.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (cantidadFotosGaleria <= 1) {
                return;
            }
            int anchoItem = getResources().getDimensionPixelSize(R.dimen.detalle_foto_tamano)
                    + getResources().getDimensionPixelSize(R.dimen.espaciado_chico);
            int indice = Math.round(scrollX / (float) anchoItem);
            indice = Math.max(0, Math.min(indice, cantidadFotosGaleria - 1));
            contadorFotos.setText(getString(R.string.detalle_foto_contador, indice + 1, cantidadFotosGaleria));
        });
    }

    /**
     * Escucha el resultado de "Gestionar publicación": la hoja ya aplicó el
     * cambio contra el repositorio, acá solo hace falta refrescar la pantalla y
     * confirmar. El Snackbar se muestra ANTES de recargar y no después: recargar
     * oculta barraAcciones mientras carga, y el Snackbar necesita una vista
     * visible para anclarse.
     */
    private void escucharResultadoDeGestion() {
        getParentFragmentManager().setFragmentResultListener(
                GestionPublicacionBottomSheet.RESULTADO_GESTION,
                getViewLifecycleOwner(),
                (clave, datos) -> {
                    int mensaje = datos.getInt(GestionPublicacionBottomSheet.EXTRA_MENSAJE);
                    if (mensaje != 0) {
                        mostrarSnackbar(getString(mensaje));
                    }
                    cargarPublicacion();
                });
    }

    /**
     * El menú se declara en el XML de la toolbar ({@code app:menu}), así que acá
     * solo hay que enganchar el listener: no se llama a inflateMenu() y no hay
     * riesgo de duplicar el ítem si onViewCreated corriera de nuevo.
     */
    private void configurarToolbar(View vista) {
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(vista).navigateUp());
        itemGuardar = toolbar.getMenu().findItem(R.id.accionGuardar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.accionGuardar) {
                alternarGuardada();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Un diálogo abierto retiene la Activity y el árbol de vistas: si el
        // fragment muere con el diálogo arriba (rotación, "atrás" del sistema)
        // queda el leak.
        if (dialogoActivo != null) {
            dialogoActivo.dismiss();
            dialogoActivo = null;
        }
        itemGuardar = null;

        toolbar = null;
        scrollContenido = null;
        scrollFotos = null;
        grupoFotos = null;
        contadorFotos = null;
        tituloDetalle = null;
        precioDetalle = null;
        estadoPublicacionDetalle = null;
        estadoDetalle = null;
        categoriaDetalle = null;
        zonaYFechaDetalle = null;
        fechaPublicacionDetalle = null;
        descripcionDetalle = null;
        bloqueMisInteracciones = null;
        grupoMisInteracciones = null;
        bloquePuntoEntrega = null;
        textoEntregaBloqueada = null;
        direccionEntregaDetalle = null;
        botonComoLlegar = null;
        tarjetaVendedor = null;
        inicialesVendedor = null;
        nombreVendedorDetalle = null;
        reputacionVendedorDetalle = null;
        miembroDesdeVendedorDetalle = null;
        botonVerPerfil = null;
        progresoInicial = null;
        estadoError = null;
        textoError = null;
        barraAcciones = null;
        textoRolAviso = null;
        accionesInteresado = null;
        botonPreguntar = null;
        botonOfertar = null;
        botonGestionar = null;
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    private void cargarPublicacion() {
        mostrarCarga();
        if (!ConectividadUtils.hayConexion(requireContext())) {
            mostrarPublicacionSinConexion();
            return;
        }
        repositorio.obtenerPublicacion(publicacionId, new RepositorioCallback<Publicacion>() {
            @Override
            public void onExito(Publicacion resultado) {
                if (scrollContenido == null) {
                    return; // la vista ya se destruyó
                }
                mostrarPublicacion(resultado);
            }

            @Override
            public void onError(String mensaje) {
                if (scrollContenido == null) {
                    return;
                }
                mostrarError(mensaje);
            }
        });
    }

    private void mostrarPublicacionSinConexion() {
        publicacionesVistas.obtenerVista(publicacionId, new RepositorioCallback<Publicacion>() {
            @Override
            public void onExito(Publicacion resultado) {
                if (scrollContenido == null) {
                    return;
                }
                mostrarPublicacion(resultado);
                mostrarSnackbar(getString(R.string.detalle_mostrando_sin_conexion));
            }

            @Override
            public void onError(String mensaje) {
                if (scrollContenido == null) {
                    return;
                }
                mostrarError(getString(R.string.detalle_sin_conexion_sin_cache));
            }
        });
    }

    private void mostrarCarga() {
        publicacionCargada = null;
        if (itemGuardar != null) {
            itemGuardar.setVisible(false);
        }
        progresoInicial.setVisibility(View.VISIBLE);
        scrollContenido.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
        barraAcciones.setVisibility(View.GONE);
    }

    private void mostrarError(String mensaje) {
        publicacionCargada = null;
        if (itemGuardar != null) {
            itemGuardar.setVisible(false);
        }
        progresoInicial.setVisibility(View.GONE);
        scrollContenido.setVisibility(View.GONE);
        barraAcciones.setVisibility(View.GONE);
        estadoError.setVisibility(View.VISIBLE);
        textoError.setText(mensaje);
    }

    private void mostrarPublicacion(Publicacion publicacion) {
        publicacionCargada = publicacion;

        mostrarGaleria(publicacion.getCantidadFotos());

        tituloDetalle.setText(publicacion.getTitulo());
        precioDetalle.setText(FormatoUtils.precio(publicacion.getPrecio()));

        // Badge de estado de la PUBLICACIÓN (no del artículo): oculto para
        // "Activa", que es el caso normal y no necesita destacarse.
        EstadoPublicacion estadoPublicacion = publicacion.getEstadoPublicacion();
        if (estadoPublicacion == EstadoPublicacion.ACTIVA) {
            estadoPublicacionDetalle.setVisibility(View.GONE);
        } else {
            estadoPublicacionDetalle.setText(estadoPublicacion.getEtiqueta());
            estadoPublicacionDetalle.setVisibility(View.VISIBLE);
        }

        estadoDetalle.setText(publicacion.getEstado().getEtiqueta());
        categoriaDetalle.setText(publicacion.getCategoria().getEtiqueta());
        zonaYFechaDetalle.setText(getString(
                R.string.item_zona_y_fecha,
                publicacion.getZona().getNombre(),
                FormatoUtils.antiguedad(requireContext(), publicacion.getFechaPublicacion())));
        fechaPublicacionDetalle.setText(getString(
                R.string.detalle_publicado_el,
                FormatoUtils.fechaCompleta(publicacion.getFechaPublicacion())));
        descripcionDetalle.setText(publicacion.getDescripcion());

        mostrarVendedor(publicacion.getVendedor());
        configurarAccionSegunRol(publicacion);

        progresoInicial.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
        scrollContenido.setVisibility(View.VISIBLE);
        barraAcciones.setVisibility(View.VISIBLE);
    }

    /**
     * Llena la galería con un placeholder por foto (sin swipe, ver
     * dimens/detalle_foto_tamano) y arma el contador ("2 de 3"), con una
     * contentDescription distinta por foto para TalkBack.
     */
    private void mostrarGaleria(int cantidadFotos) {
        grupoFotos.removeAllViews();
        cantidadFotosGaleria = Math.max(cantidadFotos, 1);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < cantidadFotosGaleria; i++) {
            ImageView foto = (ImageView) inflater.inflate(R.layout.item_foto_detalle, grupoFotos, false);
            foto.setContentDescription(getString(R.string.detalle_foto_numero, i + 1, cantidadFotosGaleria));
            grupoFotos.addView(foto);
        }
        scrollFotos.scrollTo(0, 0);

        // Con una sola foto, "1 de 1" no aporta nada.
        boolean hayVariasFotos = cantidadFotosGaleria > 1;
        contadorFotos.setVisibility(hayVariasFotos ? View.VISIBLE : View.GONE);
        if (hayVariasFotos) {
            contadorFotos.setText(getString(R.string.detalle_foto_contador, 1, cantidadFotosGaleria));
        }
    }

    private void mostrarVendedor(Vendedor vendedor) {
        inicialesVendedor.setText(TextoUtils.iniciales(vendedor.getNombre()));
        nombreVendedorDetalle.setText(vendedor.getNombre());
        VendedorUi.pintarReputacion(reputacionVendedorDetalle, vendedor);
        miembroDesdeVendedorDetalle.setText(getString(
                R.string.vendedor_miembro_desde,
                FormatoUtils.mesYAnio(vendedor.getMiembroDesde())));

        View.OnClickListener irAlPerfil = v -> irAlPerfil(vendedor.getId());
        tarjetaVendedor.setOnClickListener(irAlPerfil);
        botonVerPerfil.setOnClickListener(irAlPerfil);
    }

    /**
     * Muestra la vista de vendedor (aviso "es tu publicación" + gestión) o la de
     * interesado (preguntar / ofertar / guardar), nunca las dos. La comparación
     * es por id de vendedor: dos personas pueden llamarse igual.
     * <p>
     * Para el interesado, además depende del estado de la publicación: si ya no
     * está activa (la pausó o la vendió el dueño) no tiene sentido ofrecer
     * "Preguntar" ni "Ofertar", pero sí sigue pudiendo guardarla, porque guardar
     * es un marcador propio y no una operación con el vendedor.
     */
    private void configurarAccionSegunRol(Publicacion publicacion) {
        Vendedor vendedor = publicacion.getVendedor();
        boolean esMia = vendedor.getId().equals(SesionUsuario.getInstancia().getIdUsuario());
        boolean activa = publicacion.getEstadoPublicacion() == EstadoPublicacion.ACTIVA;

        accionesInteresado.setVisibility((!esMia && activa) ? View.VISIBLE : View.GONE);
        botonGestionar.setVisibility(esMia ? View.VISIBLE : View.GONE);
        // Nadie guarda su propia publicación.
        if (itemGuardar != null) {
            itemGuardar.setVisible(!esMia);
        }

        if (esMia) {
            textoRolAviso.setText(R.string.detalle_es_tu_publicacion);
            textoRolAviso.setVisibility(View.VISIBLE);
            botonGestionar.setOnClickListener(v ->
                    GestionPublicacionBottomSheet.nuevaInstancia(publicacionId)
                            .show(getParentFragmentManager(), GestionPublicacionBottomSheet.TAG));
            // El propio vendedor no le pregunta ni le oferta a su publicación; eso
            // lo ve del otro lado, en "Gestionar publicación".
            bloqueMisInteracciones.setVisibility(View.GONE);
            // Es su propia dirección: la ve siempre, no depende de ninguna oferta.
            mostrarPuntoEntrega(publicacion.getDireccionEntrega(), true);
            return;
        }

        actualizarIconoGuardar();
        mostrarMisInteracciones(publicacion);
        if (activa) {
            textoRolAviso.setVisibility(View.GONE);
            // Preguntar y ofertar requieren conexión.
            botonPreguntar.setOnClickListener(v -> {
                if (!ConectividadUtils.hayConexion(requireContext())) {
                    mostrarSnackbar(getString(R.string.error_accion_requiere_conexion));
                    return;
                }
                mostrarDialogoPregunta(vendedor);
            });
            botonOfertar.setOnClickListener(v -> {
                if (!ConectividadUtils.hayConexion(requireContext())) {
                    mostrarSnackbar(getString(R.string.error_accion_requiere_conexion));
                    return;
                }
                mostrarDialogoOferta(publicacion);
            });
        } else {
            textoRolAviso.setText(R.string.detalle_publicacion_no_disponible);
            textoRolAviso.setVisibility(View.VISIBLE);
        }
    }

    // ------------------------------------------------------------------
    // Lo que el interesado ya le envió al vendedor
    // ------------------------------------------------------------------

    /**
     * Muestra las preguntas y la oferta vigente que el usuario logueado ya le
     * mandó al vendedor sobre esta publicación. Si no mandó nada, el bloque
     * entero queda oculto. No se llama para el propio vendedor: sus
     * preguntas/ofertas recibidas se ven desde "Gestionar publicación", no acá.
     * <p>
     * Las dos consultas van contra el backend real y están anidadas (la de
     * oferta arranca en el {@code onExito} de la de preguntas), mismo criterio
     * que antes de migrar de Room: evita coordinar dos callbacks independientes
     * para saber cuándo terminaron los dos. El listado de preguntas es público
     * por publicación ({@code GET /publicaciones/{id}/preguntas}, se filtra
     * "las mías" del lado del cliente) y las ofertas salen de
     * {@code GET /ofertas/enviadas} (todas las del usuario, se filtra por esta
     * publicación) — mismo criterio de filtrado client-side que ya usa
     * {@code OfertasRepositoryRemoto} para separar enviadas/recibidas.
     */
    private void mostrarMisInteracciones(Publicacion publicacion) {
        String idUsuario = SesionUsuario.getInstancia().getIdUsuario();
        preguntaRepository.deLaPublicacion(publicacion.getId(),
                new RepositorioCallback<List<Pregunta>>() {
                    @Override
                    public void onExito(List<Pregunta> todas) {
                        if (grupoMisInteracciones == null) {
                            return; // la vista ya se destruyó
                        }
                        List<Pregunta> misPreguntas = new ArrayList<>();
                        for (Pregunta pregunta : todas) {
                            if (pregunta.getAutorId().equals(idUsuario)) {
                                misPreguntas.add(pregunta);
                            }
                        }
                        cargarMiOferta(publicacion, misPreguntas);
                    }

                    @Override
                    public void onError(String mensaje) {
                        // No hay nada que romper: si falla, el bloque de "lo mío"
                        // simplemente no se completa con las preguntas.
                        cargarMiOferta(publicacion, new ArrayList<>());
                    }
                });
    }

    private void cargarMiOferta(Publicacion publicacion, List<Pregunta> misPreguntas) {
        if (grupoMisInteracciones == null) {
            return;
        }
        ofertasRepository.enviadas(new RepositorioCallback<List<OfertaNegociacion>>() {
            @Override
            public void onExito(List<OfertaNegociacion> todas) {
                if (grupoMisInteracciones == null) {
                    return;
                }
                pintarMisInteracciones(publicacion, misPreguntas, ultimaOfertaDe(todas, publicacion.getId()));
            }

            @Override
            public void onError(String mensaje) {
                // Ídem: sin oferta propia, no rompe el resto del bloque.
                if (grupoMisInteracciones != null) {
                    pintarMisInteracciones(publicacion, misPreguntas, null);
                }
            }
        });
    }

    /** La más reciente entre las ofertas del usuario logueado sobre esta publicación, si hay alguna. */
    @Nullable
    private static OfertaNegociacion ultimaOfertaDe(List<OfertaNegociacion> ofertas, String publicacionId) {
        for (OfertaNegociacion oferta : ofertas) {
            // enviadas() ya viene ordenada "más recientes primero" (docs/ofertas-api.md).
            if (oferta.getPublicacionId().equals(publicacionId)) {
                return oferta;
            }
        }
        return null;
    }

    private void pintarMisInteracciones(Publicacion publicacion, List<Pregunta> misPreguntas,
                                        @Nullable OfertaNegociacion miOferta) {
        grupoMisInteracciones.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        if (!misPreguntas.isEmpty()) {
            grupoMisInteracciones.addView(
                    inflarEncabezado(inflater, R.string.detalle_mis_preguntas_titulo));
            for (Pregunta pregunta : misPreguntas) {
                grupoMisInteracciones.addView(inflarFilaInteraccion(
                        inflater, R.drawable.ic_preguntar, pregunta.getTexto(), pregunta.getFecha()));
            }
        }
        if (miOferta != null) {
            grupoMisInteracciones.addView(
                    inflarEncabezado(inflater, R.string.detalle_mi_oferta_titulo));
            // Se suma el estado (Pendiente/Aceptada/Rechazada/Vencida) junto a la
            // antigüedad para que se note en pantalla por qué se desbloqueó (o no)
            // el punto de entrega, sin tener que ir hasta "Mis ofertas".
            String antiguedadYEstado = getString(R.string.detalle_mi_oferta_estado,
                    FormatoUtils.antiguedad(requireContext(), miOferta.getFechaCreacion()),
                    getString(miOferta.getEstado().getEtiqueta()));
            View filaOferta = inflater.inflate(R.layout.item_interaccion, grupoMisInteracciones, false);
            ((ImageView) filaOferta.findViewById(R.id.iconoInteraccion)).setImageResource(R.drawable.ic_ofertar);
            ((TextView) filaOferta.findViewById(R.id.textoInteraccion))
                    .setText(FormatoUtils.precio(miOferta.getPrecio()));
            ((TextView) filaOferta.findViewById(R.id.autorInteraccion)).setText(antiguedadYEstado);
            grupoMisInteracciones.addView(filaOferta);
        }

        boolean hayAlgoQueMostrar = !misPreguntas.isEmpty() || miOferta != null;
        bloqueMisInteracciones.setVisibility(hayAlgoQueMostrar ? View.VISIBLE : View.GONE);

        // Antes de tener una oferta ACEPTADA no hay ninguna dirección que mostrar
        // ni siquiera bloqueada: el backend no la manda (routers/ofertas.py) hasta
        // ese momento, así que no hay forma de anticipar si existe una cargada.
        String direccion = puedeVerPuntoDeEntrega(miOferta) ? miOferta.getDireccionEntrega() : null;
        mostrarPuntoEntrega(direccion, direccion != null);
    }

    private View inflarEncabezado(LayoutInflater inflater, @StringRes int texto) {
        View encabezado = inflater.inflate(
                R.layout.item_encabezado_seccion, grupoMisInteracciones, false);
        ((TextView) encabezado.findViewById(R.id.textoEncabezadoSeccion)).setText(texto);
        return encabezado;
    }

    /** Fila de {@code item_interaccion.xml} para "lo mío": sin nombre de autor, solo la antigüedad. */
    private View inflarFilaInteraccion(LayoutInflater inflater,
                                       @DrawableRes int icono,
                                       String texto,
                                       long fecha) {
        View fila = inflater.inflate(R.layout.item_interaccion, grupoMisInteracciones, false);
        ((ImageView) fila.findViewById(R.id.iconoInteraccion)).setImageResource(icono);
        ((TextView) fila.findViewById(R.id.textoInteraccion)).setText(texto);
        ((TextView) fila.findViewById(R.id.autorInteraccion))
                .setText(FormatoUtils.antiguedad(requireContext(), fecha));
        return fila;
    }

    // ------------------------------------------------------------------
    // Punto de entrega y "Cómo llegar" (Punto 8)
    // ------------------------------------------------------------------

    /**
     * Pinta el bloque de punto de entrega a partir de la dirección que le pasa
     * cada caller (el dueño la tiene siempre disponible en su propia
     * publicación; el interesado recién la recibe del backend cuando su oferta
     * queda {@code ACEPTADA}, ver {@link #pintarMisInteracciones}). Si no hay
     * dirección, el bloque entero queda oculto: no tiene sentido mostrar ni
     * siquiera el aviso de "bloqueada" si no hay ninguna esperando del otro
     * lado — y para el interesado sin oferta aceptada tampoco hay forma de
     * saber si existe, porque el backend no la manda hasta ese momento.
     */
    private void mostrarPuntoEntrega(@Nullable String direccion, boolean desbloqueado) {
        if (bloquePuntoEntrega == null) {
            return; // la vista ya se destruyó
        }
        if (!MapaUtils.tieneDireccion(direccion)) {
            bloquePuntoEntrega.setVisibility(View.GONE);
            return;
        }

        bloquePuntoEntrega.setVisibility(View.VISIBLE);
        textoEntregaBloqueada.setVisibility(desbloqueado ? View.GONE : View.VISIBLE);
        direccionEntregaDetalle.setVisibility(desbloqueado ? View.VISIBLE : View.GONE);
        botonComoLlegar.setVisibility(desbloqueado ? View.VISIBLE : View.GONE);

        if (desbloqueado) {
            direccionEntregaDetalle.setText(direccion);
            botonComoLlegar.setOnClickListener(v -> {
                if (!MapaUtils.abrirComoLlegar(requireContext(), direccion)) {
                    // Pasa en un emulador sin Google Maps ni ninguna otra app de
                    // mapas instalada: no es un bug, hay que avisarle al usuario.
                    mostrarSnackbar(getString(R.string.detalle_mapa_sin_app));
                }
            });
        }
    }

    /**
     * El enunciado del Punto 8 dice "una vez aceptada una oferta". Con el
     * ciclo completo de estados del Punto 7 ya integrado, el desbloqueo es
     * literal: la oferta vigente de este usuario está en {@code ACEPTADA},
     * ya sea porque el vendedor aceptó su oferta original o porque él aceptó
     * una contraoferta del vendedor.
     */
    private boolean puedeVerPuntoDeEntrega(@Nullable OfertaNegociacion oferta) {
        return oferta != null && oferta.getEstado() == EstadoOferta.ACEPTADA;
    }

    // ------------------------------------------------------------------
    // Guardar
    // ------------------------------------------------------------------

    private void alternarGuardada() {
        if (publicacionCargada == null) {
            return; // el ítem no debería estar visible sin publicación, pero por las dudas
        }
        // Guardar la publicación requiere conexión.
        if (!ConectividadUtils.hayConexion(requireContext())) {
            mostrarSnackbar(getString(R.string.error_accion_requiere_conexion));
            return;
        }
        boolean quedaraGuardada = !favoritoRepositorio.esFavorito(publicacionCargada.getId());
        RepositorioCallback<Void> callback = new RepositorioCallback<Void>() {
            @Override
            public void onExito(Void resultado) {
                pintarIconoGuardar(quedaraGuardada);
                mostrarSnackbar(getString(quedaraGuardada
                        ? R.string.detalle_guardada_ok
                        : R.string.detalle_guardada_quitada));
            }

            @Override
            public void onError(String mensaje) {
                if (barraAcciones == null) {
                    return;
                }
                mostrarSnackbar(mensaje);
            }
        };
        if (quedaraGuardada) {
            favoritoRepositorio.marcar(publicacionCargada, callback);
        } else {
            favoritoRepositorio.desmarcar(publicacionCargada.getId(), callback);
        }
    }

    private void actualizarIconoGuardar() {
        if (itemGuardar == null) {
            return;
        }
        pintarIconoGuardar(favoritoRepositorio.esFavorito(publicacionId));
    }

    private void pintarIconoGuardar(boolean guardada) {
        if (itemGuardar == null) {
            return;
        }
        itemGuardar.setIcon(guardada ? R.drawable.ic_favorito_lleno : R.drawable.ic_favorito_borde);
        // El título es lo que anuncia TalkBack y lo que se ve al mantener presionado.
        itemGuardar.setTitle(guardada ? R.string.detalle_quitar_guardada : R.string.detalle_guardar);
    }

    // ------------------------------------------------------------------
    // Preguntar y ofertar
    // ------------------------------------------------------------------

    private void mostrarDialogoPregunta(Vendedor vendedor) {
        View contenido = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_pregunta, null, false);
        TextInputLayout input = contenido.findViewById(R.id.inputPregunta);
        TextInputEditText campo = contenido.findViewById(R.id.campoPregunta);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.detalle_pregunta_titulo, vendedor.getNombre()))
                .setView(contenido)
                .setNegativeButton(R.string.dialogo_cancelar, null)
                // Listener en null a propósito: se engancha abajo, después de show().
                .setPositiveButton(R.string.detalle_pregunta_enviar, null)
                .create();

        // Si el listener se pasara en setPositiveButton, el diálogo se cerraría
        // SIEMPRE al tocarlo y la validación no podría retener al usuario. Con el
        // OnClickListener del botón puesto después de show() decidimos nosotros
        // cuándo se cierra.
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String texto = leerTexto(campo);
                    if (texto.isEmpty()) {
                        input.setError(getString(R.string.detalle_pregunta_vacia));
                        return;
                    }
                    if (texto.length() < LARGO_MINIMO_PREGUNTA) {
                        input.setError(getString(R.string.detalle_pregunta_corta));
                        return;
                    }
                    input.setError(null);
                    dialogo.dismiss();

                    registrarPregunta(texto);
                    mostrarSnackbar(getString(R.string.detalle_pregunta_enviada, vendedor.getNombre()));
                }));

        dialogoActivo = dialogo;
        dialogo.show();
    }

    /**
     * A diferencia de la versión anterior (Room), no hay pre-chequeo local de "ya
     * ofertaste": el backend real ya valida una sola oferta PENDIENTE por
     * publicación y devuelve 409 con el mensaje si se repite (ver
     * {@code docs/ofertas-api.md}) — se muestra directo, en {@link #registrarOferta}.
     */
    private void mostrarDialogoOferta(Publicacion publicacion) {
        armarYMostrarDialogoOferta(publicacion);
    }

    private void armarYMostrarDialogoOferta(Publicacion publicacion) {
        View contenido = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_oferta, null, false);
        TextView precioPedido = contenido.findViewById(R.id.textoPrecioPedido);
        TextInputLayout input = contenido.findViewById(R.id.inputOferta);
        TextInputEditText campo = contenido.findViewById(R.id.campoOferta);
        TextInputEditText campoMensaje = contenido.findViewById(R.id.campoMensajeOferta);

        precioPedido.setText(getString(R.string.detalle_oferta_ayuda,
                FormatoUtils.precio(publicacion.getPrecio())));

        double minimo = publicacion.getPrecio() * PROPORCION_MINIMA_OFERTA;

        AlertDialog dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.detalle_oferta_titulo)
                .setView(contenido)
                .setNegativeButton(R.string.dialogo_cancelar, null)
                .setPositiveButton(R.string.detalle_oferta_enviar, null)
                .create();

        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String texto = leerTexto(campo);
                    if (texto.isEmpty()) {
                        input.setError(getString(R.string.detalle_oferta_vacia));
                        return;
                    }
                    double monto;
                    try {
                        monto = Double.parseDouble(texto);
                    } catch (NumberFormatException e) {
                        // El inputType es numérico, pero un teclado raro puede colar algo.
                        input.setError(getString(R.string.detalle_oferta_invalida));
                        return;
                    }
                    if (monto <= 0) {
                        input.setError(getString(R.string.detalle_oferta_invalida));
                        return;
                    }
                    if (monto < minimo) {
                        input.setError(getString(R.string.detalle_oferta_muy_baja,
                                FormatoUtils.precio(minimo)));
                        return;
                    }
                    if (monto > publicacion.getPrecio()) {
                        input.setError(getString(R.string.detalle_oferta_muy_alta,
                                FormatoUtils.precio(publicacion.getPrecio())));
                        return;
                    }
                    input.setError(null);
                    dialogo.dismiss();

                    String mensaje = leerTexto(campoMensaje);
                    registrarOferta(publicacion, monto, mensaje.isEmpty() ? null : mensaje);
                }));

        dialogoActivo = dialogo;
        dialogo.show();
    }

    /** Texto del campo, sin nulls ni espacios de más. */
    private String leerTexto(TextInputEditText campo) {
        CharSequence contenido = campo.getText();
        return contenido == null ? "" : contenido.toString().trim();
    }

    /** Manda la pregunta al backend real y refresca "lo que ya le enviaste" de la pantalla. */
    private void registrarPregunta(String texto) {
        if (publicacionCargada == null) {
            return; // no debería pasar: el diálogo solo se abre con una publicación cargada
        }
        preguntaRepository.crear(publicacionCargada.getId(), texto, new RepositorioCallback<Pregunta>() {
            @Override
            public void onExito(Pregunta resultado) {
                if (grupoMisInteracciones == null) {
                    return;
                }
                mostrarMisInteracciones(publicacionCargada);
            }

            @Override
            public void onError(String mensaje) {
                if (barraAcciones == null) {
                    return;
                }
                mostrarSnackbar(mensaje);
            }
        });
    }

    /** Manda la oferta al backend real (Punto 7: {@code POST /ofertas}). */
    private void registrarOferta(Publicacion publicacion, double monto, @Nullable String mensaje) {
        ofertasRepository.crear(publicacion.getId(), monto, mensaje,
                new RepositorioCallback<OfertaNegociacion>() {
                    @Override
                    public void onExito(OfertaNegociacion resultado) {
                        mostrarSnackbar(getString(R.string.detalle_oferta_enviada,
                                FormatoUtils.precio(monto), publicacion.getVendedor().getNombre()));
                    }

                    @Override
                    public void onError(String mensajeError) {
                        mostrarSnackbar(mensajeError);
                    }
                });
    }

    // ------------------------------------------------------------------
    // Navegación al perfil
    // ------------------------------------------------------------------

    private void irAlPerfil(String vendedorId) {
        Bundle argumentos = new Bundle();
        argumentos.putString(PerfilVendedorFragment.ARG_VENDEDOR_ID, vendedorId);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_detalle_to_perfil, argumentos);
    }

    /**
     * Ancla el Snackbar arriba de la barra de acciones: como esa barra es fija y
     * opaca, un Snackbar sin ancla quedaría dibujado detrás (su elevación es
     * menor a la de la barra) y nunca se vería.
     */
    private void mostrarSnackbar(String mensaje) {
        if (barraAcciones == null) {
            return; // la vista ya se destruyó (por ejemplo, desde un diálogo)
        }
        Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG)
                .setAnchorView(barraAcciones)
                .show();
    }
}
