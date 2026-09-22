package com.example.tpo.ui.detalle;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.widget.NestedScrollView;

import com.example.tpo.R;
import com.example.tpo.data.OfertasRepository;
import com.example.tpo.data.PreguntaRepository;
import com.example.tpo.data.PreguntaRepositoryApi;
import com.example.tpo.data.PublicacionRepository;
import com.example.tpo.data.PublicacionRepositoryApi;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.OfertaNegociacion;
import com.example.tpo.model.Pregunta;
import com.example.tpo.model.Publicacion;
import com.example.tpo.util.FormatoUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Hoja "Gestionar publicación" — acción del vendedor en el Detalle (Punto 4).
 * <p>
 * Recibe el id de la publicación (nunca el objeto completo, mismo criterio que
 * el resto de las pantallas) y lo resuelve otra vez contra el repositorio: así
 * siempre muestra el estado más actualizado, incluso si el Detalle quedó
 * atrás en la pila de navegación con datos ya viejos.
 * <p>
 * Cambia el estado de la publicación (pausar / reactivar / marcar vendida)
 * contra el repositorio y lista las preguntas y ofertas que recibió. No
 * muestra ella misma el Snackbar de confirmación: al terminar se cierra con
 * {@code dismiss()} y su vista deja de existir con ella, así que el aviso lo
 * da el Detalle al recibir el resultado por la Fragment Result API (mismo
 * patrón que {@link com.example.tpo.ui.home.FiltrosBottomSheet}).
 */
@AndroidEntryPoint
public class GestionPublicacionBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "GestionPublicacionBottomSheet";

    /** Clave del resultado que el Detalle escucha. */
    public static final String RESULTADO_GESTION = "resultado_gestion";
    /** Id de string con el mensaje de confirmación a mostrar, dentro del Bundle de resultado. */
    public static final String EXTRA_MENSAJE = "mensaje";

    private static final String ARG_PUBLICACION_ID = "arg_publicacion_id";

    private PublicacionRepository repositorio;
    private String publicacionId;

    private PreguntaRepository preguntaRepository;

    /** Lo inyecta Hilt: lee las ofertas recibidas contra el backend real (Punto 7). */
    @Inject
    OfertasRepository ofertasRepository;

    /** Estado con el que se mostró la publicación la última vez: decide qué botones se ven. */
    @Nullable
    private EstadoPublicacion estadoActual;

    // --- Vistas. Son null fuera del rango onCreateView..onDestroyView ---
    private NestedScrollView scrollGestion;
    private CircularProgressIndicator progresoGestion;
    private View estadoErrorGestion;
    private TextView textoErrorGestion;
    private TextView tituloGestion;
    private TextView precioGestion;
    private TextView estadoActualGestion;
    private MaterialButton botonPausar;
    private MaterialButton botonReactivar;
    private MaterialButton botonMarcarVendida;
    private MaterialButton botonVolverAPublicar;
    private TextView cantidadPreguntasGestion;
    private TextView textoSinPreguntas;
    private LinearLayout grupoPreguntasGestion;
    private TextView cantidadOfertasGestion;
    private TextView textoSinOfertas;
    private LinearLayout grupoOfertasGestion;

    /**
     * Crea la hoja para gestionar una publicación puntual.
     * <p>
     * Recibe el id y no el objeto {@link Publicacion}: si viajara el objeto, la
     * hoja podría mostrar datos desactualizados si el usuario la abre después
     * de que la publicación cambió por otro camino.
     */
    public static GestionPublicacionBottomSheet nuevaInstancia(String publicacionId) {
        GestionPublicacionBottomSheet hoja = new GestionPublicacionBottomSheet();
        Bundle argumentos = new Bundle();
        argumentos.putString(ARG_PUBLICACION_ID, publicacionId);
        hoja.setArguments(argumentos);
        return hoja;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        preguntaRepository = PreguntaRepositoryApi.getInstancia(context);
        repositorio = PublicacionRepositoryApi.getInstancia(context);
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
        return inflater.inflate(R.layout.bottom_sheet_gestion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scrollGestion = view.findViewById(R.id.scrollGestion);
        progresoGestion = view.findViewById(R.id.progresoGestion);
        estadoErrorGestion = view.findViewById(R.id.estadoErrorGestion);
        textoErrorGestion = view.findViewById(R.id.textoErrorGestion);
        tituloGestion = view.findViewById(R.id.tituloGestion);
        precioGestion = view.findViewById(R.id.precioGestion);
        estadoActualGestion = view.findViewById(R.id.estadoActualGestion);
        botonPausar = view.findViewById(R.id.botonPausar);
        botonReactivar = view.findViewById(R.id.botonReactivar);
        botonMarcarVendida = view.findViewById(R.id.botonMarcarVendida);
        botonVolverAPublicar = view.findViewById(R.id.botonVolverAPublicar);
        cantidadPreguntasGestion = view.findViewById(R.id.cantidadPreguntasGestion);
        textoSinPreguntas = view.findViewById(R.id.textoSinPreguntas);
        grupoPreguntasGestion = view.findViewById(R.id.grupoPreguntasGestion);
        cantidadOfertasGestion = view.findViewById(R.id.cantidadOfertasGestion);
        textoSinOfertas = view.findViewById(R.id.textoSinOfertas);
        grupoOfertasGestion = view.findViewById(R.id.grupoOfertasGestion);
        MaterialButton botonEditarGestion = view.findViewById(R.id.botonEditarGestion);

        botonPausar.setOnClickListener(v ->
                cambiarEstado(EstadoPublicacion.PAUSADA, R.string.gestion_pausada_ok));
        botonReactivar.setOnClickListener(v ->
                cambiarEstado(EstadoPublicacion.ACTIVA, R.string.gestion_reactivada_ok));
        botonMarcarVendida.setOnClickListener(v ->
                cambiarEstado(EstadoPublicacion.VENDIDA, R.string.gestion_vendida_ok));
        botonVolverAPublicar.setOnClickListener(v ->
                cambiarEstado(EstadoPublicacion.ACTIVA, R.string.gestion_reactivada_ok));
        // La edición real depende de cámara/galería y borrador persistente — Punto
        // 5, todavía no se vio en clase. Por ahora queda como stub explícito.
        botonEditarGestion.setOnClickListener(v ->
                Snackbar.make(view, R.string.gestion_editar_proximamente, Snackbar.LENGTH_LONG).show());
        view.findViewById(R.id.botonReintentarGestion).setOnClickListener(v -> cargarPublicacion());

        cargarPublicacion();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        scrollGestion = null;
        progresoGestion = null;
        estadoErrorGestion = null;
        textoErrorGestion = null;
        tituloGestion = null;
        precioGestion = null;
        estadoActualGestion = null;
        botonPausar = null;
        botonReactivar = null;
        botonMarcarVendida = null;
        botonVolverAPublicar = null;
        cantidadPreguntasGestion = null;
        textoSinPreguntas = null;
        grupoPreguntasGestion = null;
        cantidadOfertasGestion = null;
        textoSinOfertas = null;
        grupoOfertasGestion = null;
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    private void cargarPublicacion() {
        mostrarCarga();
        repositorio.obtenerPublicacion(publicacionId, new RepositorioCallback<Publicacion>() {
            @Override
            public void onExito(Publicacion resultado) {
                if (scrollGestion == null) {
                    return; // la vista ya se destruyó
                }
                mostrarPublicacion(resultado);
            }

            @Override
            public void onError(String mensaje) {
                if (scrollGestion == null) {
                    return;
                }
                mostrarError(mensaje);
            }
        });
    }

    private void mostrarCarga() {
        progresoGestion.setVisibility(View.VISIBLE);
        scrollGestion.setVisibility(View.GONE);
        estadoErrorGestion.setVisibility(View.GONE);
    }

    private void mostrarError(String mensaje) {
        progresoGestion.setVisibility(View.GONE);
        scrollGestion.setVisibility(View.GONE);
        estadoErrorGestion.setVisibility(View.VISIBLE);
        textoErrorGestion.setText(mensaje);
    }

    private void mostrarPublicacion(Publicacion publicacion) {
        estadoActual = publicacion.getEstadoPublicacion();

        tituloGestion.setText(publicacion.getTitulo());
        precioGestion.setText(FormatoUtils.precio(publicacion.getPrecio()));
        estadoActualGestion.setText(getString(
                R.string.gestion_estado_actual, getString(estadoActual.getEtiqueta())));

        configurarBotonesSegunEstado();
        mostrarPreguntasRecibidas();
        mostrarOfertasRecibidas();

        progresoGestion.setVisibility(View.GONE);
        estadoErrorGestion.setVisibility(View.GONE);
        scrollGestion.setVisibility(View.VISIBLE);
    }

    /**
     * Qué botones se ven según el estado actual. La máquina de estados de la
     * gestión es chica y por eso vive acá, no en el modelo:
     * <ul>
     *     <li>ACTIVA → Pausar, Marcar como vendida.</li>
     *     <li>PAUSADA → Reactivar, Marcar como vendida.</li>
     *     <li>VENDIDA → Volver a publicar (vuelve a ACTIVA).</li>
     * </ul>
     */
    private void configurarBotonesSegunEstado() {
        boolean activa = estadoActual == EstadoPublicacion.ACTIVA;
        boolean pausada = estadoActual == EstadoPublicacion.PAUSADA;
        boolean vendida = estadoActual == EstadoPublicacion.VENDIDA;

        botonPausar.setVisibility(activa ? View.VISIBLE : View.GONE);
        botonReactivar.setVisibility(pausada ? View.VISIBLE : View.GONE);
        botonMarcarVendida.setVisibility((activa || pausada) ? View.VISIBLE : View.GONE);
        botonVolverAPublicar.setVisibility(vendida ? View.VISIBLE : View.GONE);
    }

    private void mostrarPreguntasRecibidas() {
        preguntaRepository.deLaPublicacion(publicacionId, new RepositorioCallback<List<Pregunta>>() {
            @Override
            public void onExito(List<Pregunta> preguntas) {
                if (grupoPreguntasGestion == null) {
                    return; // la vista ya se destruyó
                }
                pintarPreguntasRecibidas(preguntas);
            }

            @Override
            public void onError(String mensaje) {
                // No corta la pantalla: el resto de la gestión (pausar/reactivar/vender)
                // sigue disponible aunque no se hayan podido traer las preguntas.
            }
        });
    }

    private void pintarPreguntasRecibidas(List<Pregunta> preguntas) {
        int cantidad = preguntas.size();
        cantidadPreguntasGestion.setText(getResources().getQuantityString(
                R.plurals.gestion_preguntas_cantidad, cantidad, cantidad));
        textoSinPreguntas.setVisibility(cantidad == 0 ? View.VISIBLE : View.GONE);

        grupoPreguntasGestion.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Pregunta pregunta : preguntas) {
            View fila = inflater.inflate(R.layout.item_interaccion, grupoPreguntasGestion, false);
            ((ImageView) fila.findViewById(R.id.iconoInteraccion)).setImageResource(R.drawable.ic_preguntar);
            ((TextView) fila.findViewById(R.id.textoInteraccion)).setText(pregunta.getTexto());
            ((TextView) fila.findViewById(R.id.autorInteraccion)).setText(getString(
                    R.string.item_zona_y_fecha,
                    pregunta.getAutorNombre(),
                    FormatoUtils.antiguedad(requireContext(), pregunta.getFecha())));
            grupoPreguntasGestion.addView(fila);
        }
    }

    /**
     * Ofertas recibidas de esta publicación puntual. El backend no tiene un
     * endpoint "por publicación": {@code GET /ofertas/recibidas} trae todas
     * las que le hicieron al vendedor logueado (sobre cualquiera de sus
     * publicaciones) y acá se filtra por {@code publicacionId}, mismo
     * criterio que ya usa {@code DetalleFragment} para "mi oferta".
     */
    private void mostrarOfertasRecibidas() {
        ofertasRepository.recibidas(new RepositorioCallback<List<OfertaNegociacion>>() {
            @Override
            public void onExito(List<OfertaNegociacion> todas) {
                if (grupoOfertasGestion == null) {
                    return;
                }
                List<OfertaNegociacion> deEstaPublicacion = new ArrayList<>();
                for (OfertaNegociacion oferta : todas) {
                    if (oferta.getPublicacionId().equals(publicacionId)) {
                        deEstaPublicacion.add(oferta);
                    }
                }
                pintarOfertasRecibidas(deEstaPublicacion);
            }

            @Override
            public void onError(String mensaje) {
                // Ídem mostrarPreguntasRecibidas: no corta el resto de la pantalla.
            }
        });
    }

    /**
     * Lista las ofertas recibidas en modo lectura, con su estado
     * (Pendiente/Aceptada/Rechazada/Vencida — Punto 7). Aceptar, rechazar y
     * contraofertar es acción de "Mis ofertas" → "Detalle de la oferta"
     * (Punto 7), no de esta hoja: reusa {@code item_interaccion.xml}, mismo
     * layout que el bloque de preguntas de al lado.
     */
    private void pintarOfertasRecibidas(List<OfertaNegociacion> ofertas) {
        int cantidad = ofertas.size();
        cantidadOfertasGestion.setText(getResources().getQuantityString(
                R.plurals.gestion_ofertas_cantidad, cantidad, cantidad));
        textoSinOfertas.setVisibility(cantidad == 0 ? View.VISIBLE : View.GONE);

        grupoOfertasGestion.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (OfertaNegociacion oferta : ofertas) {
            View fila = inflater.inflate(R.layout.item_interaccion, grupoOfertasGestion, false);
            ((ImageView) fila.findViewById(R.id.iconoInteraccion)).setImageResource(R.drawable.ic_ofertar);
            ((TextView) fila.findViewById(R.id.textoInteraccion)).setText(FormatoUtils.precio(oferta.getPrecio()));
            ((TextView) fila.findViewById(R.id.autorInteraccion)).setText(getString(
                    R.string.gestion_oferta_autor_fecha_estado,
                    oferta.getNombreComprador(),
                    FormatoUtils.antiguedad(requireContext(), oferta.getFechaCreacion()),
                    getString(oferta.getEstado().getEtiqueta())));

            grupoOfertasGestion.addView(fila);
        }
    }

    // ------------------------------------------------------------------
    // Cambio de estado
    // ------------------------------------------------------------------

    private void cambiarEstado(EstadoPublicacion nuevoEstado, @StringRes int mensajeConfirmacion) {
        repositorio.cambiarEstadoPublicacion(publicacionId, nuevoEstado, new RepositorioCallback<Publicacion>() {
            @Override
            public void onExito(Publicacion resultado) {
                if (scrollGestion == null) {
                    return; // la hoja ya se cerró (por ejemplo, deslizada hacia abajo)
                }
                // El Snackbar lo muestra el Detalle: si lo mostráramos acá, se
                // iría con la vista de la hoja apenas dismiss() la destruya.
                Bundle datos = new Bundle();
                datos.putInt(EXTRA_MENSAJE, mensajeConfirmacion);
                getParentFragmentManager().setFragmentResult(RESULTADO_GESTION, datos);
                dismiss();
            }

            @Override
            public void onError(String mensaje) {
                if (scrollGestion == null) {
                    return;
                }
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
