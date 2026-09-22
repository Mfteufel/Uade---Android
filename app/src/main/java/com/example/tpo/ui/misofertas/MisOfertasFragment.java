package com.example.tpo.ui.misofertas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.data.OfertasRepository;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.OfertaNegociacion;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * "Mis ofertas" (Punto 7): dos tabs sobre el mismo {@code RecyclerView} — "Enviadas"
 * (las ofertas que hice como comprador) y "Recibidas" (las que me hicieron como
 * vendedor). No usa {@code ViewPager2}: son solo dos listas que se recargan al
 * cambiar de tab, agregar esa dependencia sería de más para este caso.
 * <p>
 * El servidor resuelve el vencimiento automático en cada lectura, así que acá
 * no hace falta un paso previo para marcar vencidas antes de cargar la tab.
 * <p>
 * "Siempre actualizadas" sin {@code SwipeRefreshLayout} (no visto en la
 * materia): se recarga solo al volver a la pantalla ({@link #onResume}) y con
 * la acción "Recargar" del menú de la toolbar.
 * <p>
 * {@code generacionConsulta} descarta resultados de una tab que el usuario ya
 * abandonó — mismo criterio anti-obsolescencia que {@code HomeFragment}.
 */
@AndroidEntryPoint
public class MisOfertasFragment extends Fragment implements OfertaAdapter.OnOfertaClickListener {

    private static final int TAB_ENVIADAS = 0;
    private static final int TAB_RECIBIDAS = 1;

    private static final String RESULTADO_OFERTA = DetalleOfertaBottomSheet.RESULTADO_OFERTA;

    /** Lo inyecta Hilt: la pantalla no sabe si del otro lado hay datos falsos o Retrofit. */
    @Inject
    OfertasRepository repositorio;

    private RecyclerView listaMisOfertas;
    private CircularProgressIndicator progreso;
    private View estadoVacio;
    private TextView textoVacio;
    private TabLayout tabs;

    private OfertaAdapter adapter;

    private int tabActual = TAB_ENVIADAS;
    private int generacionConsulta = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mis_ofertas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        tabs = view.findViewById(R.id.tabsMisOfertas);
        listaMisOfertas = view.findViewById(R.id.listaMisOfertas);
        progreso = view.findViewById(R.id.progresoMisOfertas);
        estadoVacio = view.findViewById(R.id.estadoVacioMisOfertas);
        textoVacio = view.findViewById(R.id.textoVacioMisOfertas);

        toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        toolbar.inflateMenu(R.menu.menu_mis_ofertas);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menuRecargarOfertas) {
                cargarTabActual();
                return true;
            }
            return false;
        });

        adapter = new OfertaAdapter(this);
        listaMisOfertas.setLayoutManager(new LinearLayoutManager(requireContext()));
        listaMisOfertas.setAdapter(adapter);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabActual = tab.getPosition();
                cargarTabActual();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        getParentFragmentManager().setFragmentResultListener(RESULTADO_OFERTA, this,
                (requestKey, resultado) -> {
                    int mensaje = resultado.getInt(DetalleOfertaBottomSheet.EXTRA_MENSAJE);
                    if (mensaje != 0 && listaMisOfertas != null) {
                        Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show();
                    }
                    cargarTabActual();
                });

        cargarTabActual();
    }

    @Override
    public void onResume() {
        super.onResume();
        // "Siempre actualizadas" (Fase 3): recargar al volver a la pantalla, además
        // de al cambiar de tab, para que una oferta que cambió de estado en otro
        // lado (o venció) se refleje sin que el usuario tenga que hacer nada.
        if (listaMisOfertas != null) {
            cargarTabActual();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listaMisOfertas = null;
        progreso = null;
        estadoVacio = null;
        textoVacio = null;
        tabs = null;
        adapter = null;
    }

    private void cargarTabActual() {
        int generacion = ++generacionConsulta;
        boolean soyComprador = tabActual == TAB_ENVIADAS;

        progreso.setVisibility(View.VISIBLE);
        listaMisOfertas.setVisibility(View.GONE);
        estadoVacio.setVisibility(View.GONE);

        RepositorioCallback<List<OfertaNegociacion>> callback = new RepositorioCallback<List<OfertaNegociacion>>() {
            @Override
            public void onExito(List<OfertaNegociacion> ofertas) {
                if (listaMisOfertas == null || generacion != generacionConsulta) {
                    return; // la vista se destruyó, o el usuario ya cambió de tab
                }
                mostrarOfertas(ofertas, soyComprador);
            }

            @Override
            public void onError(String mensaje) {
                if (listaMisOfertas == null || generacion != generacionConsulta) {
                    return;
                }
                mostrarError(mensaje);
            }
        };

        if (soyComprador) {
            repositorio.enviadas(callback);
        } else {
            repositorio.recibidas(callback);
        }
    }

    private void mostrarOfertas(List<OfertaNegociacion> ofertas, boolean soyComprador) {
        progreso.setVisibility(View.GONE);
        adapter.reemplazar(ofertas, soyComprador);

        boolean vacio = ofertas.isEmpty();
        textoVacio.setText(soyComprador
                ? R.string.mis_ofertas_vacio_enviadas
                : R.string.mis_ofertas_vacio_recibidas);
        estadoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        listaMisOfertas.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    private void mostrarError(String mensaje) {
        progreso.setVisibility(View.GONE);
        Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG)
                .setAction(R.string.error_reintentar, v -> cargarTabActual())
                .show();
    }

    @Override
    public void onOfertaClick(OfertaNegociacion oferta) {
        DetalleOfertaBottomSheet.nuevaInstancia(oferta.getId())
                .show(getParentFragmentManager(), DetalleOfertaBottomSheet.TAG);
    }
}
