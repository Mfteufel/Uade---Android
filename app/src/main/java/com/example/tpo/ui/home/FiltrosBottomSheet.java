package com.example.tpo.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;

import com.example.tpo.R;
import com.example.tpo.data.SesionUsuario;
import com.example.tpo.model.Cercania;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.FiltroPublicaciones;
import com.example.tpo.ui.ChipsUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.EnumSet;
import java.util.Set;

/**
 * Hoja inferior con los filtros avanzados del Home: rango de precio, estado del
 * artículo y cercanía a la zona del usuario.
 * <p>
 * <b>Cómo se comunica con el Home:</b> por Fragment Result API
 * ({@code setFragmentResult} / {@code setFragmentResultListener}) y no por una
 * interfaz de callback. La diferencia importa: una interfaz guardada en un campo
 * se pierde si el sistema recrea los fragments (por ejemplo al rotar), mientras
 * que el resultado por FragmentManager sobrevive y se entrega igual.
 * <p>
 * La hoja edita una <b>copia</b> del filtro. Si el usuario toca controles y
 * cierra sin aplicar, el listado del Home queda como estaba.
 */
public class FiltrosBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "FiltrosBottomSheet";

    /** Clave del resultado que el Home escucha. */
    public static final String RESULTADO_FILTROS = "resultado_filtros";
    /** Clave del filtro dentro del Bundle de resultado. */
    public static final String EXTRA_FILTRO = "filtro";

    private static final String ARG_FILTRO = "arg_filtro";
    private static final String ESTADO_BORRADOR = "estado_borrador";

    private TextInputEditText campoPrecioDesde;
    private TextInputEditText campoPrecioHasta;
    private TextView textoPrecioInvalido;
    private ChipGroup grupoEstados;
    private ChipGroup grupoCercania;

    /** Copia editable del filtro. Solo viaja al Home cuando se toca "Aplicar". */
    private FiltroPublicaciones borrador;

    /**
     * Crea la hoja ya cargada con los filtros que el Home tiene aplicados.
     * <p>
     * Los datos van por argumentos y no por el constructor porque el sistema
     * puede recrear el fragment y en ese caso llama siempre al constructor vacío:
     * lo único que se conserva es el Bundle de argumentos.
     */
    public static FiltrosBottomSheet nuevaInstancia(FiltroPublicaciones filtroActual) {
        FiltrosBottomSheet hoja = new FiltrosBottomSheet();
        Bundle argumentos = new Bundle();
        argumentos.putSerializable(ARG_FILTRO, filtroActual.copia());
        hoja.setArguments(argumentos);
        return hoja;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Si la hoja se está recreando (rotación), se recupera lo que el usuario
        // venía editando; si es la primera vez, se parte del filtro del Home.
        FiltroPublicaciones recuperado = null;
        if (savedInstanceState != null) {
            recuperado = BundleCompat.getSerializable(
                    savedInstanceState, ESTADO_BORRADOR, FiltroPublicaciones.class);
        }
        if (recuperado == null) {
            recuperado = BundleCompat.getSerializable(
                    requireArguments(), ARG_FILTRO, FiltroPublicaciones.class);
        }
        borrador = recuperado == null ? new FiltroPublicaciones() : recuperado;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Solo inflar: nada de findViewById ni listeners acá.
        return inflater.inflate(R.layout.bottom_sheet_filtros, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        campoPrecioDesde = view.findViewById(R.id.campoPrecioDesde);
        campoPrecioHasta = view.findViewById(R.id.campoPrecioHasta);
        textoPrecioInvalido = view.findViewById(R.id.textoPrecioInvalido);
        grupoEstados = view.findViewById(R.id.grupoEstados);
        grupoCercania = view.findViewById(R.id.grupoCercania);
        TextView textoZonaUsuario = view.findViewById(R.id.textoZonaUsuario);
        MaterialButton botonLimpiar = view.findViewById(R.id.botonLimpiar);
        MaterialButton botonAplicar = view.findViewById(R.id.botonAplicar);

        textoZonaUsuario.setText(getString(
                R.string.filtros_cercania_ayuda,
                SesionUsuario.getInstancia().getZona().getNombre()));

        mostrarPrecios();
        crearChipsDeEstado();
        crearChipsDeCercania();

        botonLimpiar.setOnClickListener(v -> limpiarFormulario());
        botonAplicar.setOnClickListener(v -> aplicar());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Se guarda lo que el usuario tiene cargado en pantalla, no el borrador
        // original, para que al rotar no pierda lo que venía tocando.
        volcarFormularioEnBorrador();
        outState.putSerializable(ESTADO_BORRADOR, borrador);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Se sueltan las referencias a vistas para no dejar memory leaks.
        campoPrecioDesde = null;
        campoPrecioHasta = null;
        textoPrecioInvalido = null;
        grupoEstados = null;
        grupoCercania = null;
    }

    // ------------------------------------------------------------------
    // Armado del formulario
    // ------------------------------------------------------------------

    private void mostrarPrecios() {
        if (borrador.getPrecioMinimo() != null) {
            campoPrecioDesde.setText(String.valueOf(borrador.getPrecioMinimo().longValue()));
        }
        if (borrador.getPrecioMaximo() != null) {
            campoPrecioHasta.setText(String.valueOf(borrador.getPrecioMaximo().longValue()));
        }
    }

    /** Un chip por cada valor del enum, marcando los que ya estaban elegidos. */
    private void crearChipsDeEstado() {
        grupoEstados.removeAllViews();
        for (EstadoArticulo estado : EstadoArticulo.values()) {
            ChipsUtils.agregarChip(
                    grupoEstados,
                    getString(estado.getEtiqueta()),
                    estado,
                    borrador.getEstados().contains(estado));
        }
    }

    private void crearChipsDeCercania() {
        grupoCercania.removeAllViews();
        for (Cercania cercania : Cercania.values()) {
            ChipsUtils.agregarChip(
                    grupoCercania,
                    getString(cercania.getEtiqueta()),
                    cercania,
                    borrador.getCercania() == cercania);
        }
    }

    /** Deja el formulario en blanco. El usuario todavía tiene que tocar "Aplicar". */
    private void limpiarFormulario() {
        campoPrecioDesde.setText("");
        campoPrecioHasta.setText("");
        textoPrecioInvalido.setVisibility(View.GONE);
        borrador.limpiarFiltrosAvanzados();
        crearChipsDeEstado();
        crearChipsDeCercania();
    }

    // ------------------------------------------------------------------
    // Lectura y validación
    // ------------------------------------------------------------------

    /**
     * Lee un campo de precio.
     *
     * @return el valor cargado, o {@code null} si está vacío (que significa
     *         "sin límite de ese lado").
     */
    @Nullable
    private Double leerPrecio(TextInputEditText campo) {
        CharSequence contenido = campo.getText();
        String texto = contenido == null ? "" : contenido.toString().trim();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            // El campo es numérico por inputType, pero si algo raro llega igual
            // no se rompe: se trata como "sin límite".
            return null;
        }
    }

    private Set<EstadoArticulo> leerEstadosSeleccionados() {
        Set<EstadoArticulo> seleccionados = EnumSet.noneOf(EstadoArticulo.class);
        for (int i = 0; i < grupoEstados.getChildCount(); i++) {
            View hijo = grupoEstados.getChildAt(i);
            if (hijo instanceof Chip && ((Chip) hijo).isChecked()) {
                seleccionados.add((EstadoArticulo) hijo.getTag());
            }
        }
        return seleccionados;
    }

    /** Pasa al borrador todo lo que hay cargado en pantalla. */
    private void volcarFormularioEnBorrador() {
        if (campoPrecioDesde == null) {
            // La vista ya no existe: el borrador queda como está.
            return;
        }
        borrador.setPrecioMinimo(leerPrecio(campoPrecioDesde));
        borrador.setPrecioMaximo(leerPrecio(campoPrecioHasta));
        borrador.setEstados(leerEstadosSeleccionados());
        borrador.setCercania((Cercania) ChipsUtils.valorSeleccionado(grupoCercania));
    }

    /**
     * Valida, guarda y le devuelve el filtro al Home.
     * <p>
     * La única validación real es el rango de precio: si el mínimo es mayor que
     * el máximo el resultado sería siempre vacío, así que conviene avisarlo antes
     * de cerrar la hoja en vez de mostrar una lista sin resultados.
     */
    private void aplicar() {
        Double minimo = leerPrecio(campoPrecioDesde);
        Double maximo = leerPrecio(campoPrecioHasta);

        if (minimo != null && maximo != null && minimo > maximo) {
            textoPrecioInvalido.setVisibility(View.VISIBLE);
            return;
        }
        textoPrecioInvalido.setVisibility(View.GONE);

        volcarFormularioEnBorrador();

        Bundle resultado = new Bundle();
        resultado.putSerializable(EXTRA_FILTRO, borrador);
        getParentFragmentManager().setFragmentResult(RESULTADO_FILTROS, resultado);

        dismiss();
    }
}
