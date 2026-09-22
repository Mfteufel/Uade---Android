package com.example.tpo.ui.historial;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.data.OperacionRepository;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.FiltroOperaciones;
import com.example.tpo.model.Operacion;
import com.example.tpo.model.TipoOperacion;
import com.example.tpo.model.UsuarioResumen;
import com.example.tpo.ui.ChipsUtils;
import com.example.tpo.ui.perfil.PerfilVendedorFragment;
import com.example.tpo.util.FormatoUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Historial de operaciones concretadas — Punto 9 del TPO.
 * <p>
 * Lista las compras y ventas concretadas del usuario con fecha, artículo, monto
 * final y contraparte, separadas por tipo, y permite filtrar por tipo y por rango
 * de fechas. Desde cada una se puede calificar a la contraparte (si el servidor
 * dice que todavía se puede) o abrir su perfil público.
 * <p>
 * Arriba, aparte y sin filtros, van las ventas aceptadas que todavía esperan la
 * entrega: el comprador la confirma ahí, y recién entonces la operación queda
 * concretada y pasa al historial.
 * <p>
 * Igual que el Home, todo lo que define la lista vive en un
 * {@link FiltroOperaciones} y hay un único método de recarga.
 */
@AndroidEntryPoint
public class HistorialFragment extends Fragment implements OperacionAdapter.Listener {

    private static final String ESTADO_FILTRO = "filtro";
    private static final String TAG_CALENDARIO = "rango_fechas";
    private static final String TAG_CALIFICAR = "calificar";

    /** Lo inyecta Hilt: la pantalla no sabe si del otro lado hay un mock o Retrofit. */
    @Inject
    OperacionRepository repositorio;

    private FiltroOperaciones filtro = new FiltroOperaciones();

    // --- Vistas. Son null fuera del rango onCreateView..onDestroyView ---
    private View bloquePendientes;
    private ChipGroup grupoTipo;
    private Chip chipFechas;
    private RecyclerView lista;
    private CircularProgressIndicator progreso;
    private View estadoVacio;
    private TextView textoVacio;
    private View estadoError;
    private TextView textoError;

    private OperacionAdapter adapter;
    private OperacionAdapter adapterPendientes;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            FiltroOperaciones guardado =
                    (FiltroOperaciones) savedInstanceState.getSerializable(ESTADO_FILTRO);
            if (guardado != null) {
                filtro = guardado;
            }
        }
        // Cuando la hoja de calificar confirma, se recarga: la operación cambia de
        // "calificable" a "calificada" y eso lo tiene que decir el servidor.
        getChildFragmentManager().setFragmentResultListener(CalificarBottomSheet.RESULTADO, this,
                (clave, resultado) -> {
                    if (getView() != null) {
                        Snackbar.make(getView(), R.string.calificacion_enviada,
                                Snackbar.LENGTH_SHORT).show();
                    }
                    cargarHistorial();
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bloquePendientes = view.findViewById(R.id.bloquePendientesEntrega);
        grupoTipo = view.findViewById(R.id.grupoTipoOperacion);
        chipFechas = view.findViewById(R.id.chipFechas);
        lista = view.findViewById(R.id.listaOperaciones);
        progreso = view.findViewById(R.id.progresoHistorial);
        estadoVacio = view.findViewById(R.id.estadoVacioHistorial);
        textoVacio = view.findViewById(R.id.textoVacioHistorial);
        estadoError = view.findViewById(R.id.estadoErrorHistorial);
        textoError = view.findViewById(R.id.textoErrorHistorial);

        view.<com.google.android.material.appbar.MaterialToolbar>findViewById(R.id.toolbarHistorial)
                .setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.botonReintentarHistorial).setOnClickListener(v -> cargarTodo());

        adapter = new OperacionAdapter(this);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        lista.setAdapter(adapter);

        adapterPendientes = new OperacionAdapter(this);
        RecyclerView listaPendientes = view.findViewById(R.id.listaPendientesEntrega);
        listaPendientes.setLayoutManager(new LinearLayoutManager(requireContext()));
        listaPendientes.setAdapter(adapterPendientes);

        configurarChipsTipo();
        chipFechas.setOnClickListener(v -> abrirCalendario());
        chipFechas.setOnCloseIconClickListener(v -> {
            filtro.setRangoFechas(null, null);
            actualizarChipFechas();
            cargarHistorial();
        });
        actualizarChipFechas();

        cargarTodo();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ESTADO_FILTRO, filtro);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bloquePendientes = null;
        grupoTipo = null;
        chipFechas = null;
        lista = null;
        progreso = null;
        estadoVacio = null;
        textoVacio = null;
        estadoError = null;
        textoError = null;
        adapter = null;
        adapterPendientes = null;
    }

    // ------------------------------------------------------------------
    // Filtros
    // ------------------------------------------------------------------

    /** "Todas" lleva tag null, que es justamente "sin filtro de tipo" en {@link FiltroOperaciones}. */
    private void configurarChipsTipo() {
        ChipsUtils.agregarChip(grupoTipo, getString(R.string.historial_todas), null,
                filtro.getTipo() == null);
        for (TipoOperacion tipo : TipoOperacion.values()) {
            ChipsUtils.agregarChip(grupoTipo, getString(tipo.getEtiqueta()), tipo,
                    filtro.getTipo() == tipo);
        }
        grupoTipo.setOnCheckedStateChangeListener((grupo, ids) -> {
            filtro.setTipo((TipoOperacion) ChipsUtils.valorSeleccionado(grupo));
            cargarHistorial();
        });
    }

    /**
     * Calendario de rango de Material. No deja elegir días futuros: no puede haber
     * operaciones concretadas mañana.
     */
    private void abrirCalendario() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(R.string.historial_elegir_fechas)
                        .setCalendarConstraints(new CalendarConstraints.Builder()
                                .setValidator(DateValidatorPointBackward.now())
                                .build());
        if (filtro.getDesde() != null && filtro.getHasta() != null) {
            builder.setSelection(new Pair<>(aMedianocheUtc(filtro.getDesde()),
                    aMedianocheUtc(filtro.getHasta())));
        }
        MaterialDatePicker<Pair<Long, Long>> calendario = builder.build();
        calendario.addOnPositiveButtonClickListener(seleccion -> {
            if (seleccion == null || seleccion.first == null || seleccion.second == null) {
                return;
            }
            // Rango inclusivo: desde el comienzo del primer día hasta el final del último.
            filtro.setRangoFechas(inicioDelDiaLocal(seleccion.first), finDelDiaLocal(seleccion.second));
            actualizarChipFechas();
            cargarHistorial();
        });
        calendario.show(getChildFragmentManager(), TAG_CALENDARIO);
    }

    private void actualizarChipFechas() {
        boolean conRango = filtro.getDesde() != null && filtro.getHasta() != null;
        chipFechas.setCloseIconVisible(conRango);
        chipFechas.setText(conRango
                ? getString(R.string.historial_rango_fechas,
                FormatoUtils.fechaCorta(filtro.getDesde()),
                FormatoUtils.fechaCorta(filtro.getHasta()))
                : getString(R.string.historial_fechas));
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    /**
     * Pendientes y después el historial. Los filtros recargan solo el historial:
     * las pendientes no se filtran. Si fallan las pendientes, se muestra el error
     * en el lugar de la lista, con el mismo "Reintentar".
     */
    private void cargarTodo() {
        mostrarCarga();
        repositorio.obtenerPendientesDeEntrega(new RepositorioCallback<List<Operacion>>() {
            @Override
            public void onExito(List<Operacion> pendientes) {
                if (lista == null) {
                    return; // la vista ya se destruyó
                }
                adapterPendientes.mostrar(pendientes, false);
                bloquePendientes.setVisibility(pendientes.isEmpty() ? View.GONE : View.VISIBLE);
                cargarHistorial();
            }

            @Override
            public void onError(String mensaje) {
                if (lista == null) {
                    return;
                }
                bloquePendientes.setVisibility(View.GONE);
                mostrarError(mensaje);
            }
        });
    }

    private void cargarHistorial() {
        mostrarCarga();
        repositorio.obtenerHistorial(filtro, new RepositorioCallback<List<Operacion>>() {
            @Override
            public void onExito(List<Operacion> operaciones) {
                if (lista == null) {
                    return; // la vista ya se destruyó
                }
                mostrarOperaciones(operaciones);
            }

            @Override
            public void onError(String mensaje) {
                if (lista == null) {
                    return;
                }
                mostrarError(mensaje);
            }
        });
    }

    private void mostrarCarga() {
        progreso.setVisibility(View.VISIBLE);
        lista.setVisibility(View.GONE);
        estadoVacio.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
    }

    private void mostrarOperaciones(List<Operacion> operaciones) {
        progreso.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);

        // Con "Todas" se separa en Compras y Ventas; con un tipo elegido el
        // encabezado sobraría.
        adapter.mostrar(operaciones, filtro.getTipo() == null);

        boolean vacio = operaciones.isEmpty();
        lista.setVisibility(vacio ? View.GONE : View.VISIBLE);
        estadoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        boolean hayFiltros = filtro.getTipo() != null || filtro.tieneRangoFechas();
        textoVacio.setText(hayFiltros ? R.string.historial_vacio_con_filtros : R.string.historial_vacio);
    }

    private void mostrarError(String mensaje) {
        progreso.setVisibility(View.GONE);
        lista.setVisibility(View.GONE);
        estadoVacio.setVisibility(View.GONE);
        estadoError.setVisibility(View.VISIBLE);
        textoError.setText(mensaje);
    }

    // ------------------------------------------------------------------
    // Interacción
    // ------------------------------------------------------------------

    @Override
    public void onCalificarClick(Operacion operacion) {
        CalificarBottomSheet.nueva(operacion).show(getChildFragmentManager(), TAG_CALIFICAR);
    }

    /** Se pide confirmación: una vez registrada, la entrega no se puede deshacer. */
    @Override
    public void onConfirmarEntregaClick(Operacion operacion) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.historial_confirmar_entrega_titulo)
                .setMessage(R.string.historial_confirmar_entrega_mensaje)
                .setNegativeButton(R.string.dialogo_cancelar, null)
                .setPositiveButton(R.string.historial_confirmar_entrega,
                        (dialogo, boton) -> confirmarEntrega(operacion))
                .show();
    }

    /** Si sale bien se recarga todo: la operación deja las pendientes y pasa al historial. */
    private void confirmarEntrega(Operacion operacion) {
        repositorio.confirmarEntrega(operacion.getId(), new RepositorioCallback<Operacion>() {
            @Override
            public void onExito(Operacion actualizada) {
                if (lista == null) {
                    return; // la vista ya se destruyó
                }
                Snackbar.make(requireView(), R.string.historial_entrega_confirmada,
                        Snackbar.LENGTH_SHORT).show();
                cargarTodo();
            }

            @Override
            public void onError(String mensaje) {
                if (lista == null) {
                    return;
                }
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onContraparteClick(UsuarioResumen contraparte) {
        Bundle argumentos = new Bundle();
        argumentos.putString(PerfilVendedorFragment.ARG_VENDEDOR_ID, contraparte.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_historial_to_perfilPublico, argumentos);
    }

    // ------------------------------------------------------------------
    // Fechas del calendario
    // ------------------------------------------------------------------

    /*
     * MaterialDatePicker trabaja con la medianoche UTC de cada día elegido; las
     * operaciones tienen fechas en hora local. Se traduce por año/mes/día para que
     * elegir "16/09" signifique el 16/09 local completo y no un corrimiento de
     * tres horas.
     */

    private static long inicioDelDiaLocal(long medianocheUtc) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(medianocheUtc);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        return local.getTimeInMillis();
    }

    private static long finDelDiaLocal(long medianocheUtc) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(inicioDelDiaLocal(medianocheUtc));
        local.add(Calendar.DAY_OF_MONTH, 1);
        return local.getTimeInMillis() - 1;
    }

    private static long aMedianocheUtc(long fechaLocal) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(fechaLocal);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }
}
