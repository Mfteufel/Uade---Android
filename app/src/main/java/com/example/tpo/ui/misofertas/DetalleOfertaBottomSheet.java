package com.example.tpo.ui.misofertas;

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
import com.example.tpo.data.OfertasRepository;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.data.SesionUsuario;
import com.example.tpo.model.EstadoOferta;
import com.example.tpo.model.OfertaNegociacion;
import com.example.tpo.util.FormatoUtils;
import com.example.tpo.util.MapaUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Hoja "Detalle de la oferta" — negociación del Punto 7, abierta desde una fila de
 * {@link MisOfertasFragment}.
 * <p>
 * Recibe el id de la oferta (no el objeto), así siempre resuelve el estado más
 * actualizado contra el repositorio.
 * <p>
 * La botonera depende de {@link OfertaNegociacion#meTocaResponder} (comparando el id
 * del usuario logueado contra comprador/vendedor y el {@code turno} que manda el
 * servidor):
 * <ul>
 *     <li>Me toca responder → Aceptar, Rechazar, Contraofertar.</li>
 *     <li>Soy parte pero no me toca (estoy esperando la respuesta del otro) → solo
 *     Contraofertar (puedo insistir con otro precio aunque no me toque, igual que
 *     antes de mi propuesta).</li>
 *     <li>No está PENDIENTE, o no soy parte → solo lectura.</li>
 * </ul>
 * <p>
 * Punto 8: apenas la oferta queda ACEPTADA, el servidor manda
 * {@link OfertaNegociacion#getDireccionEntrega()} con valor — esta hoja la muestra
 * con su botón "Cómo llegar" (ver {@link #mostrarPuntoEntrega}), mismo criterio que
 * el Detalle de publicación.
 */
@AndroidEntryPoint
public class DetalleOfertaBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "DetalleOfertaBottomSheet";

    /** Clave del resultado que "Mis ofertas" escucha para refrescar y mostrar el Snackbar. */
    public static final String RESULTADO_OFERTA = "resultado_oferta";
    /** Id de string con el mensaje de confirmación, dentro del Bundle de resultado. */
    public static final String EXTRA_MENSAJE = "mensaje";

    private static final String ARG_OFERTA_ID = "arg_oferta_id";

    /** Lo inyecta Hilt: la hoja no sabe si del otro lado hay datos falsos o Retrofit. */
    @Inject
    OfertasRepository repositorio;

    private String ofertaId;

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
        repositorio.obtener(ofertaId, new RepositorioCallback<OfertaNegociacion>() {
            @Override
            public void onExito(OfertaNegociacion oferta) {
                if (scrollDetalleOferta == null) {
                    return; // la vista ya se destruyó
                }
                mostrarOferta(oferta);
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

    private void mostrarOferta(OfertaNegociacion oferta) {
        tituloPublicacionDetalleOferta.setText(oferta.getTituloPublicacion());
        montoDetalleOferta.setText(getString(
                R.string.detalle_oferta_neg_monto_actual, FormatoUtils.precio(oferta.getPrecio())));
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
                FormatoUtils.tiempoRestante(requireContext(), oferta.getVenceEn())));

        configurarBotonesSegunRolYEstado(oferta);
        mostrarPuntoEntrega(oferta);

        progresoDetalleOferta.setVisibility(View.GONE);
        estadoErrorDetalleOferta.setVisibility(View.GONE);
        scrollDetalleOferta.setVisibility(View.VISIBLE);
    }

    /**
     * Punto 8: acá es donde el comprador (o el vendedor, viendo su propia
     * dirección) se entera del punto de entrega apenas la oferta pasa a
     * ACEPTADA — mismo criterio y misma utilidad ({@link MapaUtils}) que el
     * bloque equivalente del Detalle de publicación.
     * <p>
     * El bloque solo aparece si la oferta está ACEPTADA. Dentro de ese caso,
     * si el vendedor nunca cargó una dirección al publicar, se avisa en vez de
     * ocultar todo — evita que parezca que la pantalla no cargó nada.
     */
    private void mostrarPuntoEntrega(OfertaNegociacion oferta) {
        boolean aceptada = oferta.getEstado() == EstadoOferta.ACEPTADA;
        bloquePuntoEntregaOferta.setVisibility(aceptada ? View.VISIBLE : View.GONE);
        if (!aceptada) {
            return;
        }

        String direccion = oferta.getDireccionEntrega();
        if (!MapaUtils.tieneDireccion(direccion)) {
            direccionEntregaDetalleOferta.setText(R.string.detalle_oferta_neg_sin_direccion);
            botonComoLlegarOferta.setVisibility(View.GONE);
            return;
        }

        direccionEntregaDetalleOferta.setText(direccion);
        botonComoLlegarOferta.setVisibility(View.VISIBLE);
        botonComoLlegarOferta.setOnClickListener(v -> {
            if (!MapaUtils.abrirComoLlegar(requireContext(), direccion)) {
                mostrarSnackbarSiSigueAbierta(getString(R.string.detalle_mapa_sin_app));
            }
        });
    }

    /**
     * Qué botones se ven según quién mira la oferta y de quién es el turno.
     * Las tres acciones (aceptar, rechazar, contraofertar) son solo para quien
     * tiene el turno — el backend también lo valida y devuelve 403 si no
     * ("Todavía no es tu turno de responder").
     */
    private void configurarBotonesSegunRolYEstado(OfertaNegociacion oferta) {
        String idUsuario = SesionUsuario.getInstancia().getUsuarioId();
        boolean meTocaResponder = oferta.meTocaResponder(idUsuario);

        botonAceptarOferta.setVisibility(meTocaResponder ? View.VISIBLE : View.GONE);
        botonRechazarOferta.setVisibility(meTocaResponder ? View.VISIBLE : View.GONE);
        botonContraofertar.setVisibility(meTocaResponder ? View.VISIBLE : View.GONE);

        botonAceptarOferta.setOnClickListener(v -> aceptar());
        botonRechazarOferta.setOnClickListener(v -> rechazar());
        botonContraofertar.setOnClickListener(v -> mostrarDialogoContraoferta(oferta));
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    private void aceptar() {
        repositorio.aceptar(ofertaId, new RepositorioCallback<OfertaNegociacion>() {
            @Override
            public void onExito(OfertaNegociacion resultado) {
                notificarResultadoYCerrar(R.string.detalle_oferta_neg_aceptar_ok);
            }

            @Override
            public void onError(String mensaje) {
                mostrarSnackbarSiSigueAbierta(mensaje);
            }
        });
    }

    private void rechazar() {
        repositorio.rechazar(ofertaId, new RepositorioCallback<OfertaNegociacion>() {
            @Override
            public void onExito(OfertaNegociacion resultado) {
                notificarResultadoYCerrar(R.string.detalle_oferta_neg_rechazar_ok);
            }

            @Override
            public void onError(String mensaje) {
                mostrarSnackbarSiSigueAbierta(mensaje);
            }
        });
    }

    private void mostrarDialogoContraoferta(OfertaNegociacion oferta) {
        View contenido = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_contraoferta, null, false);
        TextView textoMontoActual = contenido.findViewById(R.id.textoMontoActualContraoferta);
        TextInputLayout input = contenido.findViewById(R.id.inputContraoferta);
        TextInputEditText campo = contenido.findViewById(R.id.campoContraoferta);
        // El contrato no admite mensaje en la contraoferta (solo en la oferta original):
        // se deja el campo oculto para no prometer algo que el backend va a ignorar.
        contenido.findViewById(R.id.inputMensajeContraoferta).setVisibility(View.GONE);

        textoMontoActual.setText(getString(
                R.string.detalle_oferta_neg_monto_actual, FormatoUtils.precio(oferta.getPrecio())));

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
                    contraofertar(monto);
                }));

        dialogo.show();
    }

    private void contraofertar(double monto) {
        repositorio.contraofertar(ofertaId, monto, new RepositorioCallback<OfertaNegociacion>() {
            @Override
            public void onExito(OfertaNegociacion resultado) {
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
