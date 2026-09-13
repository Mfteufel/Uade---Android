package com.example.tpo.ui.perfil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.data.PerfilVendedor;
import com.example.tpo.data.PublicacionRepository;
import com.example.tpo.data.PublicacionRepositoryMock;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Vendedor;
import com.example.tpo.ui.VendedorUi;
import com.example.tpo.ui.home.PublicacionAdapter;
import com.example.tpo.util.FormatoUtils;
import com.example.tpo.util.TextoUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;

/**
 * Perfil público del vendedor — Punto 4 del TPO.
 * <p>
 * Recibe el id del vendedor por argumento de navegación y lo resuelve contra el
 * repositorio, igual que el Detalle. Muestra los datos del vendedor (reputación,
 * antigüedad) y el listado de sus publicaciones activas, reusando el mismo
 * {@link PublicacionAdapter} que el Home; tocar una lleva a su Detalle.
 */
public class PerfilVendedorFragment extends Fragment
        implements PublicacionAdapter.OnPublicacionClickListener {

    public static final String ARG_VENDEDOR_ID = "vendedorId";

    private final PublicacionRepository repositorio = PublicacionRepositoryMock.getInstancia();
    private String vendedorId;

    // --- Vistas. Son null fuera del rango onCreateView..onDestroyView ---
    private MaterialToolbar toolbar;
    private View contenidoPerfil;
    private TextView inicialesPerfil;
    private TextView nombrePerfil;
    private TextView reputacionPerfil;
    private TextView miembroDesdePerfil;
    private TextView cantidadPublicacionesPerfil;
    private RecyclerView listaPublicacionesVendedor;
    private View estadoVacioPerfil;
    private CircularProgressIndicator progresoInicial;
    private View estadoError;
    private TextView textoError;

    private PublicacionAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vendedorId = requireArguments().getString(ARG_VENDEDOR_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil_vendedor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.toolbar);
        contenidoPerfil = view.findViewById(R.id.contenidoPerfil);
        inicialesPerfil = view.findViewById(R.id.inicialesPerfil);
        nombrePerfil = view.findViewById(R.id.nombrePerfil);
        reputacionPerfil = view.findViewById(R.id.reputacionPerfil);
        miembroDesdePerfil = view.findViewById(R.id.miembroDesdePerfil);
        cantidadPublicacionesPerfil = view.findViewById(R.id.cantidadPublicacionesPerfil);
        listaPublicacionesVendedor = view.findViewById(R.id.listaPublicacionesVendedor);
        estadoVacioPerfil = view.findViewById(R.id.estadoVacioPerfil);
        progresoInicial = view.findViewById(R.id.progresoInicial);
        estadoError = view.findViewById(R.id.estadoError);
        textoError = view.findViewById(R.id.textoError);

        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.botonReintentar).setOnClickListener(v -> cargarPerfil());

        adapter = new PublicacionAdapter(this);
        listaPublicacionesVendedor.setLayoutManager(new LinearLayoutManager(requireContext()));
        listaPublicacionesVendedor.setAdapter(adapter);

        cargarPerfil();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        toolbar = null;
        contenidoPerfil = null;
        inicialesPerfil = null;
        nombrePerfil = null;
        reputacionPerfil = null;
        miembroDesdePerfil = null;
        cantidadPublicacionesPerfil = null;
        listaPublicacionesVendedor = null;
        estadoVacioPerfil = null;
        progresoInicial = null;
        estadoError = null;
        textoError = null;
        adapter = null;
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    private void cargarPerfil() {
        mostrarCarga();
        repositorio.obtenerPerfilVendedor(vendedorId, new RepositorioCallback<PerfilVendedor>() {
            @Override
            public void onExito(PerfilVendedor resultado) {
                if (listaPublicacionesVendedor == null) {
                    return; // la vista ya se destruyó
                }
                mostrarPerfil(resultado);
            }

            @Override
            public void onError(String mensaje) {
                if (listaPublicacionesVendedor == null) {
                    return;
                }
                mostrarError(mensaje);
            }
        });
    }

    private void mostrarCarga() {
        progresoInicial.setVisibility(View.VISIBLE);
        contenidoPerfil.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
    }

    private void mostrarError(String mensaje) {
        progresoInicial.setVisibility(View.GONE);
        contenidoPerfil.setVisibility(View.GONE);
        estadoError.setVisibility(View.VISIBLE);
        textoError.setText(mensaje);
    }

    private void mostrarPerfil(PerfilVendedor perfil) {
        Vendedor vendedor = perfil.getVendedor();
        inicialesPerfil.setText(TextoUtils.iniciales(vendedor.getNombre()));
        nombrePerfil.setText(vendedor.getNombre());
        VendedorUi.pintarReputacion(reputacionPerfil, vendedor);
        miembroDesdePerfil.setText(getString(
                R.string.vendedor_miembro_desde,
                FormatoUtils.mesYAnio(vendedor.getMiembroDesde())));

        int cantidad = perfil.getPublicaciones().size();
        cantidadPublicacionesPerfil.setText(getResources().getQuantityString(
                R.plurals.perfil_publicaciones_cantidad, cantidad, cantidad));

        adapter.reemplazar(perfil.getPublicaciones());
        boolean vacio = cantidad == 0;
        estadoVacioPerfil.setVisibility(vacio ? View.VISIBLE : View.GONE);
        listaPublicacionesVendedor.setVisibility(vacio ? View.GONE : View.VISIBLE);
        cantidadPublicacionesPerfil.setVisibility(vacio ? View.GONE : View.VISIBLE);

        progresoInicial.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
        contenidoPerfil.setVisibility(View.VISIBLE);
    }

    /** El usuario tocó una publicación del listado: se va a su Detalle, igual que desde el Home. */
    @Override
    public void onPublicacionClick(Publicacion publicacion) {
        Bundle argumentos = new Bundle();
        argumentos.putString("publicacionId", publicacion.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_perfil_to_detalle, argumentos);
    }
}
