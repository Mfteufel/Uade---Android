package com.example.tpo.ui.misofertas;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;

import com.example.tpo.R;
import com.example.tpo.data.OfertasPublicacion;
import com.example.tpo.data.PublicacionRepository;
import com.example.tpo.data.PublicacionRepositoryMock;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.data.SesionUsuario;
import com.example.tpo.model.EstadoOferta;
import com.example.tpo.model.Oferta;
import com.example.tpo.model.Publicacion;
import com.example.tpo.util.FormatoUtils;
import com.example.tpo.util.MapaUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Hoja "Detalle de la oferta" — negociación del Punto 7, abierta desde una fila de
 * {@link MisOfertasFragment}.
 * <p>
 * Recibe el id de la oferta (no el objeto), mismo criterio que
 * {@code GestionPublicacionBottomSheet} con el id de la publicación: siempre
 * resuelve el estado más actualizado contra Room.
 * <p>
 * La botonera depende del rol de quien mira (comparando ids contra
 * {@link SesionUsuario}) y del estado actual:
 * <ul>
 *     <li>Soy el vendedor y está PENDIENTE → Aceptar, Rechazar, Contraofertar.</li>
 *     <li>Soy el comprador, está PENDIENTE y la última propuesta es del vendedor
 *     (me contraofertó) → Aceptar esa contraoferta, o Contraofertar de nuevo.</li>
 *     <li>Cualquier otro caso (terminal, o estoy esperando respuesta) → solo lectura.</li>
 * </ul>
 * <p>
 * Punto 8: apenas la oferta queda ACEPTADA, esta hoja también muestra el punto de
 * entrega de la publicación con su botón "Cómo llegar" (ver {@link #mostrarPuntoEntrega}) —
 * mismo bloque, mismo criterio, que el Detalle de publicación.
 */
public class DetalleOfertaBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "DetalleOfertaBottomSheet";

    /** Clave del resultado que "Mis ofertas" escucha para refrescar y mostrar el Snackbar. */
    public static final String RESULTADO_OFERTA = "resultado_oferta";
    /** Id de string con el mensaje de confirmación, dentro del Bundle de resultado. */
    public static final String EXTRA_MENSAJE = "mensaje";

    private static final String ARG_OFERTA_ID = "arg_oferta_id";

    private final PublicacionRepository repositorio = PublicacionRepositoryMock.getInstancia();
    private String ofertaId;

    private OfertasPublicacion ofertasPublicacion;

    @Nullable
    private Oferta ofertaActual;

    private NestedScrollView scrollDetalleOferta;
    private CircularProgressIndicator progresoDetalleOferta;
    private View estadoErrorDetalleOferta;
    private TextView textoErrorDetalleOferta;
    private TextView tituloPublicacionDetalleOferta;
    private TextView montoDetalleOferta;
    private TextView estadoDetalleOferta;
    private TextView mensajeDetalleOferta;
    private TextView vencimientoDetalleOferta;
    private View bloquePuntoEntregaOferta;
    private TextView direccionEntregaDetalleOferta;
    private MaterialButton botonComoLlegarOferta;
    private MaterialButton botonAceptarOferta;
    private MaterialButton botonRechazarOferta;
    private MaterialButton botonContraofertar;

    public static DetalleOfertaBottomSheet nuevaInstancia(String ofertaId) {
        DetalleOfertaBottomSheet hoja = new DetalleOfertaBottomSheet();
        Bundle argumentos = new Bundle();
        argumentos.putString(ARG_OFERTA_ID, ofertaId);
        hoja.setArguments(argumentos);
        return hoja;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        ofertasPublicacion = OfertasPublicacion.getInstancia(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ofertaId = requireArguments().getString(ARG_OFERTA_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_detalle_oferta, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scrollDetalleOferta = view.findViewById(R.id.scrollDetalleOferta);
        progresoDetalleOferta = view.findViewById(R.id.progresoDetalleOferta);
        estadoErrorDetalleOferta = view.findViewById(R.id.estadoErrorDetalleOferta);
        textoErrorDetalleOferta = view.findViewById(R.id.textoErrorDetalleOferta);
        tituloPublicacionDetalleOferta = view.findViewById(R.id.tituloPublicacionDetalleOferta);
        montoDetalleOferta = view.findViewById(R.id.montoDetalleOferta);
        estadoDetalleOferta = view.findViewById(R.id.estadoDetalleOferta);
        mensajeDetalleOferta = view.findViewById(R.id.mensajeDetalleOferta);
        vencimientoDetalleOferta = view.findViewById(R.id.vencimientoDetalleOferta);
        bloquePuntoEntregaOferta = view.findViewById(R.id.bloquePuntoEntregaOferta);
        direccionEntregaDetalleOferta = view.findViewById(R.id.direccionEntregaDetalleOferta);
        botonComoLlegarOferta = view.findViewById(R.id.botonComoLlegarOferta);
        botonAceptarOferta = view.findViewById(R.id.botonAceptarOferta);
        botonRechazarOferta = view.findViewById(R.id.botonRechazarOferta);
        botonContraofertar = view.findViewById(R.id.botonContraofertar);

        view.findViewById(R.id.botonReintentarDetalleOferta).setOnClickListener(v -> cargarOferta());

        cargarOferta();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        scrollDetalleOferta = null;
        progresoDetalleOferta = null;
        estadoErrorDetalleOferta = null;
        textoErrorDetalleOferta = null;
        tituloPublicacionDetalleOferta = null;
        montoDetalleOferta = null;
        estadoDetalleOferta = null;
        mensajeDetalleOferta = null;
        vencimientoDetalleOferta = null;
        bloquePuntoEntregaOferta = null;
        direccionEntregaDetalleOferta = null;
        botonComoLlegarOferta = null;
        botonAceptarOferta = null;
        botonRechazarOferta = null;
        botonContraofertar = null;
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    private void cargarOferta() {
        mostrarCarga();
        ofertasPublicacion.obtenerPorId(ofertaId, new RepositorioCallback<Oferta>() {
            @Override
            public void onExito(Oferta oferta) {
                if (scrollDetalleOferta == null) {
                    return; // la vista ya se destruyó
                }
                ofertaActual = oferta;
                repositorio.obtenerPublicacion(oferta.getPublicacionId(),
                        new RepositorioCallback<Publicacion>() {
                            @Override
                            public void onExito(Publicacion publicacion) {
                                if (scrollDetalleOferta == null) {
                                    return;
                                }
                                mostrarOferta(oferta, publicacion);
                            }

                            @Override
                            public void onError(String mensaje) {
                                if (scrollDetalleOferta == null) {
                                    return;
                                }
                                mostrarError(mensaje);
                            }
                        });
            }

            @Override
            public void onError(String mensaje) {
                if (scrollDetalleOferta == null) {
                    return;
                }
                mostrarError(mensaje);
            }
        });
    }

    private void mostrarCarga() {
        progresoDetalleOferta.setVisibility(View.VISIBLE);
        scrollDetalleOferta.setVisibility(View.GONE);
        estadoErrorDetalleOferta.setVisibility(View.GONE);
    }

    private void mostrarError(String mensaje) {
        progresoDetalleOferta.setVisibility(View.GONE);
        scrollDetalleOferta.setVisibility(View.GONE);
        estadoErrorDetalleOferta.setVisibility(View.VISIBLE);
        textoErrorDetalleOferta.setText(mensaje);
    }

    private void mostrarOferta(Oferta oferta, Publicacion publicacion) {
        tituloPublicacionDetalleOferta.setText(publicacion.getTitulo());
        montoDetalleOferta.setText(getString(
                R.string.detalle_oferta_neg_monto_actual, FormatoUtils.precio(oferta.getMonto())));
        estadoDetalleOferta.setText(getString(
                R.string.detalle_oferta_neg_estado, getString(oferta.getEstado().getEtiqueta())));

        String mensaje = oferta.getMensaje();
        if (mensaje != null && !mensaje.isEmpty()) {
            mensajeDetalleOferta.setText(getString(R.string.detalle_oferta_neg_mensaje, mensaje));
            mensajeDetalleOferta.setVisibility(View.VISIBLE);
        } else {
            mensajeDetalleOferta.setVisibility(View.GONE);
        }

        vencimientoDetalleOferta.setText(getString(R.string.detalle_oferta_neg_vencimiento,
                FormatoUtils.tiempoRestante(requireContext(), oferta.getFechaVencimiento())));

        configurarBotonesSegunRolYEstado(oferta, publicacion);
        mostrarPuntoEntrega(oferta, publicacion);

        progresoDetalleOferta.setVisibility(View.GONE);
        estadoErrorDetalleOferta.setVisibility(View.GONE);
        scrollDetalleOferta.setVisibility(View.VISIBLE);
    }

    /**
     * Punto 8: acá es donde el comprador (o el vendedor, viendo su propia
     * dirección) se entera del punto de entrega apenas la oferta pasa a
     * ACEPTADA — mismo criterio y misma utilidad ({@link MapaUtils}) que el
     * bloque equivalente del Detalle de publicación.
     */
    private void mostrarPuntoEntrega(Oferta oferta, Publicacion publicacion) {
        String direccion = publicacion.getDireccionEntrega();
        boolean desbloqueado = oferta.getEstado() == EstadoOferta.ACEPTADA
                && MapaUtils.tieneDireccion(direccion);

        bloquePuntoEntregaOferta.setVisibility(desbloqueado ? View.VISIBLE : View.GONE);
        if (!desbloqueado) {
            return;
        }

        direccionEntregaDetalleOferta.setText(direccion);
        botonComoLlegarOferta.setOnClickListener(v -> {
            if (!MapaUtils.abrirComoLlegar(requireContext(), direccion)) {
                mostrarSnackbarSiSigueAbierta(getString(R.string.detalle_mapa_sin_app));
            }
        });
    }

    /**
     * Qué botones se ven según quién mira la oferta y su estado actual. Solo hay
     * acciones posibles cuando la oferta sigue {@code PENDIENTE}: aceptar y rechazar
     * están disponibles para cualquiera de las dos partes (quien no propuso el último
     * monto es quien tiene que responder), contraofertar también para las dos.
     */
    private void configurarBotonesSegunRolYEstado(Oferta oferta, Publicacion publicacion) {
        String idUsuario = SesionUsuario.getInstancia().getIdUsuario();
        boolean soyParte = idUsuario.equals(oferta.getAutorId()) || idUsuario.equals(oferta.getVendedorId());
        boolean pendiente = oferta.getEstado() == EstadoOferta.PENDIENTE;
        // Quien propuso el último monto espera respuesta de la otra parte: no puede
        // aceptar ni rechazar su propia propuesta, pero sí puede seguir contraofertando.
        boolean esperandoMiRespuesta = pendiente && soyParte && !idUsuario.equals(oferta.getPropuestoPor());

        botonAceptarOferta.setVisibility(esperandoMiRespuesta ? View.VISIBLE : View.GONE);
        botonRechazarOferta.setVisibility(esperandoMiRespuesta ? View.VISIBLE : View.GONE);
        botonContraofertar.setVisibility(pendiente && soyParte ? View.VISIBLE : View.GONE);

        botonAceptarOferta.setOnClickListener(v -> aceptar());
        botonRechazarOferta.setOnClickListener(v -> rechazar());
        botonContraofertar.setOnClickListener(v -> mostrarDialogoContraoferta(oferta, publicacion));
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    private void aceptar() {
        ofertasPublicacion.aceptar(ofertaId, new RepositorioCallback<Void>() {
            @Override
            public void onExito(Void resultado) {
                notificarResultadoYCerrar(R.string.detalle_oferta_neg_aceptar_ok);
            }

            @Override
            public void onError(String mensaje) {
                mostrarSnackbarSiSigueAbierta(mensaje);
            }
        });
    }

    private void rechazar() {
        ofertasPublicacion.rechazar(ofertaId, new RepositorioCallback<Void>() {
            @Override
            public void onExito(Void resultado) {
                notificarResultadoYCerrar(R.string.detalle_oferta_neg_rechazar_ok);
            }

            @Override
            public void onError(String mensaje) {
                mostrarSnackbarSiSigueAbierta(mensaje);
            }
        });
    }

    private void mostrarDialogoContraoferta(Oferta oferta, Publicacion publicacion) {
        View contenido = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_contraoferta, null, false);
        TextView textoMontoActual = contenido.findViewById(R.id.textoMontoActualContraoferta);
        TextInputLayout input = contenido.findViewById(R.id.inputContraoferta);
        TextInputEditText campo = contenido.findViewById(R.id.campoContraoferta);
        TextInputEditText campoMensaje = contenido.findViewById(R.id.campoMensajeContraoferta);

        textoMontoActual.setText(getString(
                R.string.detalle_oferta_neg_monto_actual, FormatoUtils.precio(oferta.getMonto())));

        AlertDialog dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.detalle_oferta_neg_contraoferta_titulo)
                .setView(contenido)
                .setNegativeButton(R.string.dialogo_cancelar, null)
                .setPositiveButton(R.string.detalle_oferta_neg_contraofertar, null)
                .create();

        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String texto = leerTexto(campo);
                    if (texto.isEmpty()) {
                        input.setError(getString(R.string.detalle_oferta_neg_monto_vacio));
                        return;
                    }
                    double monto;
                    try {
                        monto = Double.parseDouble(texto);
                    } catch (NumberFormatException e) {
                        input.setError(getString(R.string.detalle_oferta_neg_monto_invalido));
                        return;
                    }
                    if (monto <= 0) {
                        input.setError(getString(R.string.detalle_oferta_neg_monto_invalido));
                        return;
                    }
                    input.setError(null);
                    dialogo.dismiss();

                    String mensaje = leerTexto(campoMensaje);
                    contraofertar(oferta, monto, mensaje.isEmpty() ? null : mensaje);
                }));

        dialogo.show();
    }

    private void contraofertar(Oferta oferta, double monto, @Nullable String mensaje) {
        String idUsuario = SesionUsuario.getInstancia().getIdUsuario();
        ofertasPublicacion.contraofertar(oferta.getId(), monto, idUsuario, mensaje,
                new RepositorioCallback<Void>() {
                    @Override
                    public void onExito(Void resultado) {
                        notificarResultadoYCerrar(R.string.detalle_oferta_neg_contraoferta_ok);
                    }

                    @Override
                    public void onError(String mensajeError) {
                        mostrarSnackbarSiSigueAbierta(mensajeError);
                    }
                });
    }

    private String leerTexto(TextInputEditText campo) {
        CharSequence contenido = campo.getText();
        return contenido == null ? "" : contenido.toString().trim();
    }

    private void notificarResultadoYCerrar(int mensajeConfirmacion) {
        if (scrollDetalleOferta == null) {
            return; // la hoja ya se cerró (por ejemplo, deslizada hacia abajo)
        }
        Bundle datos = new Bundle();
        datos.putInt(EXTRA_MENSAJE, mensajeConfirmacion);
        getParentFragmentManager().setFragmentResult(RESULTADO_OFERTA, datos);
        dismiss();
    }

    private void mostrarSnackbarSiSigueAbierta(String mensaje) {
        if (scrollDetalleOferta == null) {
            return;
        }
        Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show();
    }
}
