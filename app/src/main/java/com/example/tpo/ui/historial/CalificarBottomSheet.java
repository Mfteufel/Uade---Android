package com.example.tpo.ui.historial;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;

import com.example.tpo.R;
import com.example.tpo.data.OperacionRepository;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.Calificacion;
import com.example.tpo.model.Operacion;
import com.example.tpo.util.FormatoUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Hoja para calificar a la contraparte de una operación (Punto 9): estrellas de
 * 1 a 5 y comentario breve opcional.
 * <p>
 * La pantalla solo pone límites de forma (mínimo una estrella, 280 caracteres):
 * las reglas de negocio (dentro de los 7 días, una sola vez, ser parte de la
 * operación) las valida el repositorio / servidor. Si rechaza, el motivo se
 * muestra en la hoja sin cerrarla, así el usuario no pierde lo que escribió.
 * <p>
 * Se comunica con el Historial por Fragment Result API, igual que
 * {@code FiltrosBottomSheet} con el Home: sobrevive a que el sistema recree los
 * fragments, cosa que un callback guardado en un campo no.
 */
@AndroidEntryPoint
public class CalificarBottomSheet extends BottomSheetDialogFragment {

    /** Clave del resultado que escucha el Historial cuando la calificación se guardó. */
    public static final String RESULTADO = "resultado_calificacion";

    private static final String ARG_OPERACION = "arg_operacion";

    /** Lo inyecta Hilt: la hoja no sabe si del otro lado hay un mock o Retrofit. */
    @Inject
    OperacionRepository repositorio;

    private Operacion operacion;

    // --- Vistas. Son null fuera del rango onCreateView..onDestroyView ---
    private RatingBar barraEstrellas;
    private TextInputEditText campoComentario;
    private TextView textoError;
    private LinearProgressIndicator progreso;
    private MaterialButton botonEnviar;
    private MaterialButton botonCancelar;

    /**
     * Crea la hoja para una operación. Va por argumentos y no por constructor: si
     * el sistema recrea el fragment, solo conserva el Bundle de argumentos.
     */
    public static CalificarBottomSheet nueva(Operacion operacion) {
        CalificarBottomSheet hoja = new CalificarBottomSheet();
        Bundle argumentos = new Bundle();
        argumentos.putSerializable(ARG_OPERACION, operacion);
        hoja.setArguments(argumentos);
        return hoja;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        operacion = BundleCompat.getSerializable(requireArguments(), ARG_OPERACION, Operacion.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_calificar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView titulo = view.findViewById(R.id.textoTituloCalificar);
        TextView articulo = view.findViewById(R.id.textoArticuloCalificar);
        TextView plazo = view.findViewById(R.id.textoPlazoCalificar);
        barraEstrellas = view.findViewById(R.id.barraCalificar);
        campoComentario = view.findViewById(R.id.campoComentario);
        textoError = view.findViewById(R.id.textoErrorCalificar);
        progreso = view.findViewById(R.id.progresoCalificar);
        botonEnviar = view.findViewById(R.id.botonEnviarCalificar);
        botonCancelar = view.findViewById(R.id.botonCancelarCalificar);

        titulo.setText(getString(R.string.calificar_titulo, operacion.getContraparte().getNombre()));
        articulo.setText(getString(R.string.calificacion_articulo, operacion.getTituloArticulo()));
        if (operacion.getCalificableHasta() != null) {
            plazo.setText(getString(R.string.historial_calificar_hasta,
                    FormatoUtils.fechaCorta(operacion.getCalificableHasta())));
        } else {
            plazo.setVisibility(View.GONE);
        }

        // Sin estrellas no hay calificación: "Enviar" se habilita recién con 1.
        barraEstrellas.setOnRatingBarChangeListener((barra, valor, delUsuario) -> {
            actualizarBotonEnviar();
            textoError.setVisibility(View.GONE);
        });
        actualizarBotonEnviar();

        botonCancelar.setOnClickListener(v -> dismiss());
        botonEnviar.setOnClickListener(v -> enviar());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        barraEstrellas = null;
        campoComentario = null;
        textoError = null;
        progreso = null;
        botonEnviar = null;
        botonCancelar = null;
    }

    private void actualizarBotonEnviar() {
        botonEnviar.setEnabled(estrellasElegidas() >= Calificacion.ESTRELLAS_MINIMAS);
    }

    private int estrellasElegidas() {
        return Math.round(barraEstrellas.getRating());
    }

    private void enviar() {
        int estrellas = estrellasElegidas();
        CharSequence texto = campoComentario.getText();
        String comentario = TextUtils.isEmpty(texto) ? null : texto.toString().trim();

        // Mientras viaja, nada se puede tocar: un doble toque mandaría dos
        // calificaciones (el servidor rechazaría la segunda, pero mejor no llegar ahí).
        setEnviando(true);
        repositorio.calificar(operacion.getId(), estrellas, comentario,
                new RepositorioCallback<Operacion>() {
                    @Override
                    public void onExito(Operacion actualizada) {
                        if (!isAdded()) {
                            return;
                        }
                        Bundle resultado = new Bundle();
                        resultado.putString(ARG_OPERACION, actualizada.getId());
                        getParentFragmentManager().setFragmentResult(RESULTADO, resultado);
                        dismiss();
                    }

                    @Override
                    public void onError(String mensaje) {
                        if (barraEstrellas == null) {
                            return; // la hoja ya se cerró
                        }
                        setEnviando(false);
                        textoError.setText(mensaje);
                        textoError.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setEnviando(boolean enviando) {
        progreso.setVisibility(enviando ? View.VISIBLE : View.GONE);
        barraEstrellas.setIsIndicator(enviando);
        campoComentario.setEnabled(!enviando);
        botonCancelar.setEnabled(!enviando);
        if (enviando) {
            botonEnviar.setEnabled(false);
        } else {
            actualizarBotonEnviar();
        }
        textoError.setVisibility(View.GONE);
    }
}
