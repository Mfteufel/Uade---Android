package com.example.tpo.ui.detalle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.tpo.R;
import com.example.tpo.data.PublicacionRepository;
import com.example.tpo.data.PublicacionRepositoryMock;
import com.example.tpo.data.PublicacionesGuardadas;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.data.SesionUsuario;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Vendedor;
import com.example.tpo.ui.VendedorUi;
import com.example.tpo.ui.perfil.PerfilVendedorFragment;
import com.example.tpo.util.FormatoUtils;
import com.example.tpo.util.TextoUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Detalle de publicación — Punto 4 del TPO.
 * <p>
 * Recibe el id de la publicación por argumento de navegación (nunca el objeto
 * completo) y lo resuelve contra el repositorio, igual que hace el Home: así el
 * día que exista la API_Rest el cambio es solo la implementación del
 * repositorio, no esta pantalla.
 * <p>
 * Muestra galería (lista simple de placeholders), datos de la publicación, fecha
 * de publicación, tarjeta del vendedor (reputación + acceso al perfil público) y
 * las acciones que cambian según quién mira: un interesado puede preguntar,
 * ofertar y guardar la publicación; el propio vendedor accede a la gestión de su
 * publicación. El rol se decide comparando el id del vendedor contra el del
 * usuario logueado (no el nombre, que puede repetirse entre personas distintas).
 */
public class DetalleFragment extends Fragment {

    private static final String ARG_PUBLICACION_ID = "publicacionId";

    /** Mínimo de caracteres para que una pregunta tenga algo de contexto. */
    private static final int LARGO_MINIMO_PREGUNTA = 10;
    /** La oferta tiene que llegar al menos a esta fracción del precio publicado. */
    private static final double PROPORCION_MINIMA_OFERTA = 0.5;

    private final PublicacionRepository repositorio = PublicacionRepositoryMock.getInstancia();
    private String publicacionId;

    /** Última publicación cargada. La usan el ítem de menú y los diálogos, que viven fuera del callback. */
    @Nullable
    private Publicacion publicacionCargada;

    // --- Vistas. Son null fuera del rango onCreateView..onDestroyView ---
    private MaterialToolbar toolbar;
    private NestedScrollView scrollContenido;
    private LinearLayout grupoFotos;
    private TextView tituloDetalle;
    private TextView precioDetalle;
    private TextView estadoDetalle;
    private TextView categoriaDetalle;
    private TextView zonaYFechaDetalle;
    private TextView fechaPublicacionDetalle;
    private TextView descripcionDetalle;
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
        grupoFotos = view.findViewById(R.id.grupoFotos);
        tituloDetalle = view.findViewById(R.id.tituloDetalle);
        precioDetalle = view.findViewById(R.id.precioDetalle);
        estadoDetalle = view.findViewById(R.id.estadoDetalle);
        categoriaDetalle = view.findViewById(R.id.categoriaDetalle);
        zonaYFechaDetalle = view.findViewById(R.id.zonaYFechaDetalle);
        fechaPublicacionDetalle = view.findViewById(R.id.fechaPublicacionDetalle);
        descripcionDetalle = view.findViewById(R.id.descripcionDetalle);
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
        view.findViewById(R.id.botonReintentar).setOnClickListener(v -> cargarPublicacion());

        cargarPublicacion();
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
        grupoFotos = null;
        tituloDetalle = null;
        precioDetalle = null;
        estadoDetalle = null;
        categoriaDetalle = null;
        zonaYFechaDetalle = null;
        fechaPublicacionDetalle = null;
        descripcionDetalle = null;
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

    /** Llena la galería con un placeholder por foto (sin swipe, ver dimens/detalle_foto_tamano). */
    private void mostrarGaleria(int cantidadFotos) {
        grupoFotos.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int fotos = Math.max(cantidadFotos, 1);
        for (int i = 0; i < fotos; i++) {
            grupoFotos.addView(inflater.inflate(R.layout.item_foto_detalle, grupoFotos, false));
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
     */
    private void configurarAccionSegunRol(Publicacion publicacion) {
        Vendedor vendedor = publicacion.getVendedor();
        boolean esMia = vendedor.getId().equals(SesionUsuario.getInstancia().getIdUsuario());

        textoRolAviso.setVisibility(esMia ? View.VISIBLE : View.GONE);
        accionesInteresado.setVisibility(esMia ? View.GONE : View.VISIBLE);
        botonGestionar.setVisibility(esMia ? View.VISIBLE : View.GONE);
        // Nadie guarda su propia publicación.
        if (itemGuardar != null) {
            itemGuardar.setVisible(!esMia);
        }

        if (esMia) {
            // La gestión de publicaciones es otro punto del TPO: por ahora es un stub.
            botonGestionar.setOnClickListener(v -> mostrarSnackbar(
                    getString(R.string.detalle_gestionar_proximamente)));
        } else {
            actualizarIconoGuardar();
            botonPreguntar.setOnClickListener(v -> mostrarDialogoPregunta(vendedor));
            botonOfertar.setOnClickListener(v -> mostrarDialogoOferta(publicacion));
        }
    }

    // ------------------------------------------------------------------
    // Guardar
    // ------------------------------------------------------------------

    private void alternarGuardada() {
        if (publicacionCargada == null) {
            return; // el ítem no debería estar visible sin publicación, pero por las dudas
        }
        boolean quedoGuardada = PublicacionesGuardadas.getInstancia()
                .alternar(publicacionCargada.getId());
        actualizarIconoGuardar();
        mostrarSnackbar(getString(quedoGuardada
                ? R.string.detalle_guardada_ok
                : R.string.detalle_guardada_quitada));
    }

    /**
     * Pinta el ítem del menú según el estado guardado. El estado sale del
     * singleton, no de la vista: por eso al volver a entrar al Detalle el
     * bookmark ya aparece lleno sin hacer nada especial.
     */
    private void actualizarIconoGuardar() {
        if (itemGuardar == null) {
            return;
        }
        boolean guardada = PublicacionesGuardadas.getInstancia().estaGuardada(publicacionId);
        itemGuardar.setIcon(guardada ? R.drawable.ic_guardar_lleno : R.drawable.ic_guardar_borde);
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
                    mostrarSnackbar(getString(R.string.detalle_pregunta_enviada, vendedor.getNombre()));
                }));

        dialogoActivo = dialogo;
        dialogo.show();
    }

    private void mostrarDialogoOferta(Publicacion publicacion) {
        View contenido = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_oferta, null, false);
        TextView precioPedido = contenido.findViewById(R.id.textoPrecioPedido);
        TextInputLayout input = contenido.findViewById(R.id.inputOferta);
        TextInputEditText campo = contenido.findViewById(R.id.campoOferta);

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
                    mostrarSnackbar(getString(R.string.detalle_oferta_enviada,
                            FormatoUtils.precio(monto), publicacion.getVendedor().getNombre()));
                }));

        dialogoActivo = dialogo;
        dialogo.show();
    }

    /** Texto del campo, sin nulls ni espacios de más. */
    private String leerTexto(TextInputEditText campo) {
        CharSequence contenido = campo.getText();
        return contenido == null ? "" : contenido.toString().trim();
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
