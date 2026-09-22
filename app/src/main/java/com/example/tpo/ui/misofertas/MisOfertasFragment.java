package com.example.tpo.ui.misofertas;

import android.content.Context;
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
import com.example.tpo.data.OfertasPublicacion;
import com.example.tpo.data.PublicacionRepository;
import com.example.tpo.data.PublicacionRepositoryApi;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.data.SesionUsuario;
import com.example.tpo.model.Oferta;
import com.example.tpo.model.Publicacion;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * "Mis ofertas" (Punto 7): dos tabs sobre el mismo {@code RecyclerView} — "Enviadas"
 * (las ofertas que hice como comprador) y "Recibidas" (las que me hicieron como
 * vendedor). No usa {@code ViewPager2}: son solo dos listas que se recargan al
 * cambiar de tab, agregar esa dependencia sería de más para este caso.
 * <p>
 * Al entrar se marcan como vencidas las ofertas pendientes cuyo plazo ya pasó (ver
 * {@link OfertasPublicacion#marcarVencidas}), antes de cargar la tab activa.
 * <p>
 * {@code generacionConsulta} descarta resultados de una tab que el usuario ya
 * abandonó — mismo criterio anti-obsolescencia que {@code HomeFragment}.
 */
public class MisOfertasFragment extends Fragment implements OfertaAdapter.OnOfertaClickListener {

    private static final int TAB_ENVIADAS = 0;
    private static final int TAB_RECIBIDAS = 1;

    private static final String RESULTADO_OFERTA = DetalleOfertaBottomSheet.RESULTADO_OFERTA;

    private PublicacionRepository repositorio;

    private OfertasPublicacion ofertasPublicacion;

    private RecyclerView listaMisOfertas;
    private CircularProgressIndicator progreso;
    private View estadoVacio;
    private TextView textoVacio;
    private TabLayout tabs;

    private OfertaAdapter adapter;

    private int tabActual = TAB_ENVIADAS;
    private int generacionConsulta = 0;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        ofertasPublicacion = OfertasPublicacion.getInstancia(context);
        repositorio = PublicacionRepositoryApi.getInstancia(context);
    }

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

        ofertasPublicacion.marcarVencidas(new RepositorioCallback<Void>() {
            @Override
            public void onExito(Void resultado) {
                if (listaMisOfertas == null) {
                    return; // la vista ya se destruyó
                }
                cargarTabActual();
            }

            @Override
            public void onError(String mensaje) {
                if (listaMisOfertas == null) {
                    return;
                }
                cargarTabActual();
            }
        });
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

        String idUsuario = SesionUsuario.getInstancia().getIdUsuario();
        RepositorioCallback<List<Oferta>> callback = new RepositorioCallback<List<Oferta>>() {
            @Override
            public void onExito(List<Oferta> ofertas) {
                if (listaMisOfertas == null || generacion != generacionConsulta) {
                    return; // la vista se destruyó, o el usuario ya cambió de tab
                }
                combinarConPublicaciones(ofertas, soyComprador, generacion);
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
            ofertasPublicacion.misOfertasComoComprador(idUsuario, callback);
        } else {
            ofertasPublicacion.misOfertasComoVendedor(idUsuario, callback);
        }
    }

    /** Segundo paso de la carga: resuelve la publicación de cada oferta contra el catálogo. */
    private void combinarConPublicaciones(List<Oferta> ofertas, boolean soyComprador, int generacion) {
        List<String> idsPublicaciones = new ArrayList<>();
        for (Oferta oferta : ofertas) {
            idsPublicaciones.add(oferta.getPublicacionId());
        }
        repositorio.obtenerVarias(idsPublicaciones, new RepositorioCallback<List<Publicacion>>() {
            @Override
            public void onExito(List<Publicacion> publicaciones) {
                if (listaMisOfertas == null || generacion != generacionConsulta) {
                    return;
                }
                List<FilaOferta> filas = new ArrayList<>();
                for (Oferta oferta : ofertas) {
                    Publicacion publicacion = buscarPorId(publicaciones, oferta.getPublicacionId());
                    if (publicacion != null) {
                        filas.add(new FilaOferta(oferta, publicacion));
                    }
                }
                mostrarFilas(filas, soyComprador);
            }

            @Override
            public void onError(String mensaje) {
                if (listaMisOfertas == null || generacion != generacionConsulta) {
                    return;
                }
                mostrarError(mensaje);
            }
        });
    }

    @Nullable
    private static Publicacion buscarPorId(List<Publicacion> publicaciones, String id) {
        for (Publicacion publicacion : publicaciones) {
            if (publicacion.getId().equals(id)) {
                return publicacion;
            }
        }
        return null;
    }

    private void mostrarFilas(List<FilaOferta> filas, boolean soyComprador) {
        progreso.setVisibility(View.GONE);
        adapter.reemplazar(filas, soyComprador);

        boolean vacio = filas.isEmpty();
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
    public void onOfertaClick(FilaOferta fila) {
        DetalleOfertaBottomSheet.nuevaInstancia(fila.getOferta().getId())
                .show(getParentFragmentManager(), DetalleOfertaBottomSheet.TAG);
    }
}
