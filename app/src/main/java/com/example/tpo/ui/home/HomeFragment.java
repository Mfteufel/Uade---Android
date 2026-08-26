package com.example.tpo.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.data.PaginaPublicaciones;
import com.example.tpo.data.PublicacionRepository;
import com.example.tpo.data.PublicacionRepositoryMock;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.Categoria;
import com.example.tpo.model.FiltroPublicaciones;
import com.example.tpo.model.OrdenPublicaciones;
import com.example.tpo.model.Publicacion;
import com.example.tpo.ui.ChipsUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Home / Explorar publicaciones — Punto 3 del TPO.
 * <p>
 * Resuelve los cuatro requisitos del enunciado:
 * <ul>
 *     <li>Listado paginado con título, precio, estado y zona.</li>
 *     <li>Buscador por texto libre sobre título y descripción.</li>
 *     <li>Filtros combinados por categoría, rango de precio, estado y cercanía.</li>
 *     <li>Ordenamiento por más recientes, menor precio o mayor precio.</li>
 * </ul>
 * <p>
 * Todo el estado de la pantalla se concentra en un único {@link FiltroPublicaciones}:
 * cada control (buscador, chips, hoja de filtros) modifica ese objeto y después
 * pide una recarga. Así hay un solo camino de datos y no una rama distinta por
 * cada control.
 */
public class HomeFragment extends Fragment implements PublicacionAdapter.OnPublicacionClickListener {

    /** Clave con la que se guarda el filtro al recrearse la pantalla (rotación). */
    private static final String ESTADO_FILTRO = "estado_filtro";

    /**
     * Espera antes de disparar la búsqueda mientras el usuario escribe.
     * Sin esto se lanzaría una consulta por cada tecla. Contra la API real eso
     * sería una request por letra.
     */
    private static final long DEMORA_BUSQUEDA_MS = 350;

    /**
     * Cuántos ítems antes del final se empieza a pedir la página siguiente.
     * Con un margen de 3 la carga arranca antes de que el usuario toque fondo.
     */
    private static final int UMBRAL_PAGINACION = 3;

    // --- Vistas. Son null fuera del rango onCreateView..onDestroyView ---
    private MaterialToolbar toolbar;
    private FloatingActionButton fabPublicar;
    private TextInputEditText campoBuscar;
    private ChipGroup grupoCategorias;
    private ChipGroup grupoOrden;
    private MaterialButton botonFiltros;
    private TextView textoResultados;
    private RecyclerView listaPublicaciones;
    private CircularProgressIndicator progresoInicial;
    private LinearProgressIndicator progresoPaginacion;
    private View estadoVacio;
    private View estadoError;
    private TextView textoError;

    private PublicacionAdapter adapter;

    // --- Estado de la pantalla (sobrevive a la destrucción de las vistas) ---
    private final PublicacionRepository repositorio = PublicacionRepositoryMock.getInstancia();
    private FiltroPublicaciones filtro = new FiltroPublicaciones();

    private int paginaActual = 0;
    private boolean hayMasPaginas = true;
    private boolean cargando = false;

    /**
     * Número de la búsqueda vigente.
     * <p>
     * Se incrementa en cada recarga. Cada consulta se acuerda del número que
     * tenía cuando salió y, al volver, lo compara: si no coincide es una
     * respuesta vieja y se descarta.
     * <p>
     * Sin esto habría un bug real: si el usuario escribe "note" y enseguida
     * agrega "book", salen dos consultas; si la primera tarda más que la segunda,
     * la pantalla terminaría mostrando los resultados de "note" con "notebook"
     * escrito en el buscador.
     */
    private int generacionConsulta = 0;

    // Handler del Main Thread usado para la espera del buscador.
    private final Handler handlerBusqueda = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable busquedaPendiente;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Al rotar, el fragment se recrea: se recupera el filtro para no perder
        // la búsqueda que el usuario tenía puesta.
        if (savedInstanceState != null) {
            FiltroPublicaciones guardado = BundleCompat.getSerializable(
                    savedInstanceState, ESTADO_FILTRO, FiltroPublicaciones.class);
            if (guardado != null) {
                filtro = guardado;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Acá solo se infla el layout. Los findViewById y los listeners van en
        // onViewCreated, cuando la vista ya existe.
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.toolbar);
        fabPublicar = view.findViewById(R.id.fabPublicar);
        campoBuscar = view.findViewById(R.id.campoBuscar);
        grupoCategorias = view.findViewById(R.id.grupoCategorias);
        grupoOrden = view.findViewById(R.id.grupoOrden);
        botonFiltros = view.findViewById(R.id.botonFiltros);
        textoResultados = view.findViewById(R.id.textoResultados);
        listaPublicaciones = view.findViewById(R.id.listaPublicaciones);
        progresoInicial = view.findViewById(R.id.progresoInicial);
        progresoPaginacion = view.findViewById(R.id.progresoPaginacion);
        estadoVacio = view.findViewById(R.id.estadoVacio);
        estadoError = view.findViewById(R.id.estadoError);
        textoError = view.findViewById(R.id.textoError);

        configurarLista();
        configurarBuscador();
        crearChipsDeCategoria();
        crearChipsDeOrden();
        configurarBotones();
        configurarEntradaAPublicar();
        escucharResultadoDeFiltros();

        actualizarBotonFiltros();
        recargarDesdeCero();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ESTADO_FILTRO, filtro);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // 1) Se cancela la búsqueda que estuviera esperando: si se disparara con
        //    las vistas ya destruidas, reventaría con NullPointerException.
        if (busquedaPendiente != null) {
            handlerBusqueda.removeCallbacks(busquedaPendiente);
            busquedaPendiente = null;
        }

        // 2) Se invalidan las consultas en vuelo. No se pueden cancelar, pero al
        //    cambiar la generación sus respuestas quedan descartadas.
        generacionConsulta++;
        cargando = false;

        // 3) Se sueltan las referencias a vistas. El Fragment puede seguir vivo
        //    después de que su vista muere; si guardara las referencias, mantendría
        //    en memoria todo el árbol de vistas (memory leak).
        toolbar = null;
        fabPublicar = null;
        campoBuscar = null;
        grupoCategorias = null;
        grupoOrden = null;
        botonFiltros = null;
        textoResultados = null;
        listaPublicaciones = null;
        progresoInicial = null;
        progresoPaginacion = null;
        estadoVacio = null;
        estadoError = null;
        textoError = null;
        adapter = null;
    }

    // ------------------------------------------------------------------
    // Configuración de la UI
    // ------------------------------------------------------------------

    private void configurarLista() {
        adapter = new PublicacionAdapter(this);
        listaPublicaciones.setLayoutManager(new LinearLayoutManager(requireContext()));
        listaPublicaciones.setAdapter(adapter);
        // El RecyclerView ocupa siempre el mismo espacio en pantalla (no cambia de
        // tamaño según el contenido), así que se le puede avisar y ahorra medidas.
        listaPublicaciones.setHasFixedSize(true);

        // Paginación: cuando el scroll se acerca al final se pide la página siguiente.
        listaPublicaciones.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) {
                    return; // solo interesa cuando el usuario baja
                }
                if (cargando || !hayMasPaginas || adapter == null) {
                    return;
                }
                LinearLayoutManager layoutManager =
                        (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) {
                    return;
                }
                int ultimoVisible = layoutManager.findLastVisibleItemPosition();
                if (ultimoVisible >= adapter.getItemCount() - UMBRAL_PAGINACION) {
                    cargarPagina(paginaActual + 1);
                }
            }
        });
    }

    private void configurarBuscador() {
        campoBuscar.setText(filtro.getTexto());
        campoBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No se usa.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No se usa.
            }

            @Override
            public void afterTextChanged(Editable s) {
                final String texto = s.toString();

                // Se descarta la búsqueda anterior que todavía no se disparó y se
                // programa una nueva: solo se consulta cuando el usuario deja de
                // escribir por DEMORA_BUSQUEDA_MS.
                if (busquedaPendiente != null) {
                    handlerBusqueda.removeCallbacks(busquedaPendiente);
                }
                busquedaPendiente = () -> {
                    // Si el texto no cambió respecto de lo ya aplicado no se
                    // recarga: evita una consulta al restaurar el campo por código.
                    if (filtro.getTexto().equals(texto.trim())) {
                        return;
                    }
                    filtro.setTexto(texto.trim());
                    recargarDesdeCero();
                };
                handlerBusqueda.postDelayed(busquedaPendiente, DEMORA_BUSQUEDA_MS);
            }
        });
    }

    /**
     * Arma los chips de categoría: uno por valor del enum más "Todas".
     * <p>
     * El listener se registra recién al final, después de crear los chips: si se
     * registrara antes, marcar el chip inicial dispararía una recarga de más.
     */
    private void crearChipsDeCategoria() {
        grupoCategorias.removeAllViews();

        // "Todas" lleva tag null, que es justamente lo que el filtro entiende
        // como "sin filtrar por categoría".
        ChipsUtils.agregarChip(grupoCategorias, getString(R.string.categoria_todas),
                null, filtro.getCategoria() == null);

        for (Categoria categoria : Categoria.values()) {
            ChipsUtils.agregarChip(grupoCategorias, getString(categoria.getEtiqueta()),
                    categoria, filtro.getCategoria() == categoria);
        }

        grupoCategorias.setOnCheckedStateChangeListener((grupo, marcados) -> {
            Categoria seleccionada = (Categoria) ChipsUtils.valorSeleccionado(grupo);
            // Si ya es la categoría aplicada no hay nada que hacer. Esto también
            // absorbe los cambios que hace el propio código (ver limpiarTodo).
            if (seleccionada == filtro.getCategoria()) {
                return;
            }
            filtro.setCategoria(seleccionada);
            recargarDesdeCero();
        });
    }

    private void crearChipsDeOrden() {
        grupoOrden.removeAllViews();

        for (OrdenPublicaciones orden : OrdenPublicaciones.values()) {
            ChipsUtils.agregarChip(grupoOrden, getString(orden.getEtiqueta()),
                    orden, filtro.getOrden() == orden);
        }

        grupoOrden.setOnCheckedStateChangeListener((grupo, marcados) -> {
            OrdenPublicaciones seleccionado = (OrdenPublicaciones) ChipsUtils.valorSeleccionado(grupo);
            if (seleccionado == null || seleccionado == filtro.getOrden()) {
                return;
            }
            filtro.setOrden(seleccionado);
            recargarDesdeCero();
        });
    }

    private void configurarBotones() {
        botonFiltros.setOnClickListener(v ->
                FiltrosBottomSheet.nuevaInstancia(filtro)
                        .show(getParentFragmentManager(), FiltrosBottomSheet.TAG));

        requireView().findViewById(R.id.botonReintentar)
                .setOnClickListener(v -> recargarDesdeCero());

        requireView().findViewById(R.id.botonLimpiarFiltros)
                .setOnClickListener(v -> limpiarTodosLosFiltros());
    }

    /**
     * Cablea las dos entradas del Punto 5: el FAB que abre el wizard de
     * publicar y el ítem "Mis publicaciones" del menú de la toolbar.
     */
    private void configurarEntradaAPublicar() {
        fabPublicar.setOnClickListener(v -> NavHostFragment.findNavController(this)
                .navigate(R.id.action_home_to_publicar));

        toolbar.inflateMenu(R.menu.menu_home);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menuMisPublicaciones) {
                NavHostFragment.findNavController(this).navigate(R.id.action_home_to_misPublicaciones);
                return true;
            }
            return false;
        });
    }

    /**
     * Escucha el filtro que devuelve el bottom sheet.
     * <p>
     * Se pasa {@code getViewLifecycleOwner()} para que el listener se dé de baja
     * solo cuando la vista muere: no hay que acordarse de removerlo a mano.
     */
    private void escucharResultadoDeFiltros() {
        getParentFragmentManager().setFragmentResultListener(
                FiltrosBottomSheet.RESULTADO_FILTROS,
                getViewLifecycleOwner(),
                (clave, datos) -> {
                    FiltroPublicaciones aplicado = BundleCompat.getSerializable(
                            datos, FiltrosBottomSheet.EXTRA_FILTRO, FiltroPublicaciones.class);
                    if (aplicado == null) {
                        return;
                    }
                    // Solo se copian los filtros que edita la hoja. El texto, la
                    // categoría y el orden los maneja esta pantalla y podrían haber
                    // cambiado mientras la hoja estaba abierta.
                    filtro.setEstados(aplicado.getEstados());
                    filtro.setPrecioMinimo(aplicado.getPrecioMinimo());
                    filtro.setPrecioMaximo(aplicado.getPrecioMaximo());
                    filtro.setCercania(aplicado.getCercania());

                    actualizarBotonFiltros();
                    recargarDesdeCero();
                });
    }

    /** Muestra en el botón cuántos filtros avanzados hay activos: "Filtros (2)". */
    private void actualizarBotonFiltros() {
        int activos = filtro.contarFiltrosAvanzadosActivos();
        botonFiltros.setText(activos == 0
                ? getString(R.string.home_filtros)
                : getString(R.string.home_filtros_con_contador, activos));
    }

    /** Deja la pantalla como recién abierta, salvo el criterio de ordenamiento. */
    private void limpiarTodosLosFiltros() {
        // Primero se actualiza el filtro y después la UI: así los listeners de los
        // chips ven que el valor ya coincide y no disparan recargas extra.
        filtro.setTexto("");
        filtro.setCategoria(null);
        filtro.limpiarFiltrosAvanzados();

        campoBuscar.setText("");
        crearChipsDeCategoria();
        actualizarBotonFiltros();
        recargarDesdeCero();
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    /** Vuelve a la página 0 con el filtro actual. Es el único punto de entrada de una búsqueda nueva. */
    private void recargarDesdeCero() {
        generacionConsulta++;   // invalida lo que esté en vuelo
        cargando = false;
        paginaActual = 0;
        hayMasPaginas = true;
        cargarPagina(0);
    }

    private void cargarPagina(int pagina) {
        if (cargando || listaPublicaciones == null) {
            return;
        }
        cargando = true;

        final boolean esPrimeraPagina = (pagina == 0);
        // Se guarda a qué búsqueda pertenece esta consulta (ver generacionConsulta).
        final int generacion = generacionConsulta;

        if (esPrimeraPagina) {
            mostrarCargaInicial();
        } else {
            progresoPaginacion.setVisibility(View.VISIBLE);
        }

        repositorio.buscarPublicaciones(filtro, pagina, new RepositorioCallback<PaginaPublicaciones>() {
            @Override
            public void onExito(PaginaPublicaciones resultado) {
                if (respuestaObsoleta(generacion)) {
                    return;
                }
                cargando = false;
                mostrarResultado(resultado);
            }

            @Override
            public void onError(String mensaje) {
                if (respuestaObsoleta(generacion)) {
                    return;
                }
                cargando = false;
                mostrarError(esPrimeraPagina, mensaje);
            }
        });
    }

    /**
     * true si esta respuesta ya no sirve: o la vista se destruyó, o el usuario
     * cambió la búsqueda mientras la consulta viajaba.
     */
    private boolean respuestaObsoleta(int generacion) {
        return listaPublicaciones == null || generacion != generacionConsulta;
    }

    private void mostrarResultado(PaginaPublicaciones resultado) {
        progresoInicial.setVisibility(View.GONE);
        progresoPaginacion.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);

        if (resultado.getPagina() == 0) {
            adapter.reemplazar(resultado.getPublicaciones());
            // Con una búsqueda nueva el scroll vuelve arriba; si no, quedaría a
            // mitad de una lista que ya no es la misma.
            if (!resultado.getPublicaciones().isEmpty()) {
                listaPublicaciones.scrollToPosition(0);
            }
        } else {
            adapter.agregar(resultado.getPublicaciones());
        }

        paginaActual = resultado.getPagina();
        hayMasPaginas = resultado.hayMas();

        int total = resultado.getTotalResultados();
        textoResultados.setText(getResources().getQuantityString(
                R.plurals.home_resultados, total, total));

        boolean sinResultados = adapter.getItemCount() == 0;
        estadoVacio.setVisibility(sinResultados ? View.VISIBLE : View.GONE);
        listaPublicaciones.setVisibility(sinResultados ? View.GONE : View.VISIBLE);
    }

    private void mostrarError(boolean esPrimeraPagina, String mensaje) {
        progresoInicial.setVisibility(View.GONE);
        progresoPaginacion.setVisibility(View.GONE);

        if (esPrimeraPagina) {
            // No hay nada en pantalla: se ocupa todo el espacio con el error.
            estadoVacio.setVisibility(View.GONE);
            listaPublicaciones.setVisibility(View.GONE);
            estadoError.setVisibility(View.VISIBLE);
            textoError.setText(mensaje);
        } else {
            // Ya hay resultados a la vista: se avisa sin tapar la lista.
            Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG)
                    .setAction(R.string.error_reintentar, v -> cargarPagina(paginaActual + 1))
                    .show();
        }
    }

    private void mostrarCargaInicial() {
        progresoInicial.setVisibility(View.VISIBLE);
        progresoPaginacion.setVisibility(View.GONE);
        listaPublicaciones.setVisibility(View.GONE);
        estadoVacio.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
    }

    // ------------------------------------------------------------------
    // Interacción con la lista
    // ------------------------------------------------------------------

    /**
     * El usuario tocó una publicación.
     * <p>
     * TODO (Punto 4 — Detalle de publicación): cuando exista DetalleFragment en el
     * nav_graph, reemplazar el Snackbar por la navegación real:
     * <pre>
     * Bundle argumentos = new Bundle();
     * argumentos.putString("publicacionId", publicacion.getId());
     * Navigation.findNavController(requireView())
     *         .navigate(R.id.action_home_to_detalle, argumentos);
     * </pre>
     */
    @Override
    public void onPublicacionClick(Publicacion publicacion) {
        Snackbar.make(
                requireView(),
                getString(R.string.detalle_proximamente, publicacion.getTitulo()),
                Snackbar.LENGTH_SHORT).show();
    }
}
