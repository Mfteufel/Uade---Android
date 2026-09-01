package com.example.tpo.ui.detalle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.tpo.R;
import com.example.tpo.data.PublicacionRepository;
import com.example.tpo.data.PublicacionRepositoryMock;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.data.SesionUsuario;
import com.example.tpo.model.Publicacion;
import com.example.tpo.util.FormatoUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

/**
 * Detalle de publicación — Punto 4 del TPO.
 * <p>
 * Recibe el id de la publicación por argumento de navegación (nunca el objeto
 * completo) y lo resuelve contra el repositorio, igual que hace el Home: así el
 * día que exista la API_Rest el cambio es solo la implementación del
 * repositorio, no esta pantalla.
 * <p>
 * Muestra galería (lista simple de placeholders), datos de la publicación,
 * datos del vendedor y una acción que cambia según el rol del usuario logueado:
 * "Contactar al vendedor" para cualquier otro usuario, o "Gestionar
 * publicación" si el usuario logueado es quien la publicó.
 */
public class DetalleFragment extends Fragment {

    private static final String ARG_PUBLICACION_ID = "publicacionId";

    private final PublicacionRepository repositorio = PublicacionRepositoryMock.getInstancia();
    private String publicacionId;

    // --- Vistas. Son null fuera del rango onCreateView..onDestroyView ---
    private MaterialToolbar toolbar;
    private NestedScrollView scrollContenido;
    private LinearLayout grupoFotos;
    private TextView tituloDetalle;
    private TextView precioDetalle;
    private TextView estadoDetalle;
    private TextView categoriaDetalle;
    private TextView zonaYFechaDetalle;
    private TextView descripcionDetalle;
    private TextView nombreVendedorDetalle;
    private CircularProgressIndicator progresoInicial;
    private View estadoError;
    private TextView textoError;
    private View barraAcciones;
    private TextView textoRolAviso;
    private MaterialButton botonAccion;

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
        descripcionDetalle = view.findViewById(R.id.descripcionDetalle);
        nombreVendedorDetalle = view.findViewById(R.id.nombreVendedorDetalle);
        progresoInicial = view.findViewById(R.id.progresoInicial);
        estadoError = view.findViewById(R.id.estadoError);
        textoError = view.findViewById(R.id.textoError);
        barraAcciones = view.findViewById(R.id.barraAcciones);
        textoRolAviso = view.findViewById(R.id.textoRolAviso);
        botonAccion = view.findViewById(R.id.botonAccion);

        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.botonReintentar).setOnClickListener(v -> cargarPublicacion());

        cargarPublicacion();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        toolbar = null;
        scrollContenido = null;
        grupoFotos = null;
        tituloDetalle = null;
        precioDetalle = null;
        estadoDetalle = null;
        categoriaDetalle = null;
        zonaYFechaDetalle = null;
        descripcionDetalle = null;
        nombreVendedorDetalle = null;
        progresoInicial = null;
        estadoError = null;
        textoError = null;
        barraAcciones = null;
        textoRolAviso = null;
        botonAccion = null;
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
        progresoInicial.setVisibility(View.VISIBLE);
        scrollContenido.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
        barraAcciones.setVisibility(View.GONE);
    }

    private void mostrarError(String mensaje) {
        progresoInicial.setVisibility(View.GONE);
        scrollContenido.setVisibility(View.GONE);
        barraAcciones.setVisibility(View.GONE);
        estadoError.setVisibility(View.VISIBLE);
        textoError.setText(mensaje);
    }

    private void mostrarPublicacion(Publicacion publicacion) {
        mostrarGaleria(publicacion.getCantidadFotos());

        tituloDetalle.setText(publicacion.getTitulo());
        precioDetalle.setText(FormatoUtils.precio(publicacion.getPrecio()));
        estadoDetalle.setText(publicacion.getEstado().getEtiqueta());
        categoriaDetalle.setText(publicacion.getCategoria().getEtiqueta());
        zonaYFechaDetalle.setText(getString(
                R.string.item_zona_y_fecha,
                publicacion.getZona().getNombre(),
                FormatoUtils.antiguedad(requireContext(), publicacion.getFechaPublicacion())));
        descripcionDetalle.setText(publicacion.getDescripcion());
        nombreVendedorDetalle.setText(publicacion.getNombreVendedor());

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

    /**
     * Define la acción del pie según quién mira la publicación: si el nombre del
     * vendedor coincide con el usuario logueado, es su propia publicación.
     * <p>
     * Ni la mensajería ni la gestión de publicaciones existen todavía (son otro
     * punto del TPO), así que ambas acciones quedan como stub con Snackbar.
     */
    private void configurarAccionSegunRol(Publicacion publicacion) {
        boolean esVendedor = publicacion.getNombreVendedor()
                .equals(SesionUsuario.getInstancia().getNombre());

        textoRolAviso.setVisibility(esVendedor ? View.VISIBLE : View.GONE);

        if (esVendedor) {
            botonAccion.setText(R.string.detalle_gestionar_publicacion);
            botonAccion.setOnClickListener(v -> mostrarSnackbar(
                    getString(R.string.detalle_gestionar_proximamente)));
        } else {
            botonAccion.setText(R.string.detalle_contactar_vendedor);
            botonAccion.setOnClickListener(v -> mostrarSnackbar(
                    getString(R.string.detalle_contactar_proximamente, publicacion.getNombreVendedor())));
        }
    }

    /**
     * Ancla el Snackbar arriba de la barra de acciones: como esa barra es fija y
     * opaca, un Snackbar sin ancla quedaría dibujado detrás (su elevación es
     * menor a la de la barra) y nunca se vería.
     */
    private void mostrarSnackbar(String mensaje) {
        Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG)
                .setAnchorView(barraAcciones)
                .show();
    }
}
