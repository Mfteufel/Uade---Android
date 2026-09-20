package com.example.tpo.ui.guardados;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.data.FavoritoRepository;
import com.example.tpo.data.FavoritoRepositoryMock;
import com.example.tpo.data.PublicacionRepository;
import com.example.tpo.data.PublicacionRepositoryMock;
import com.example.tpo.data.PublicacionesGuardadas;
import com.example.tpo.data.PublicacionesVistas;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.Publicacion;
import com.example.tpo.ui.home.PublicacionAdapter;
import com.example.tpo.util.ConectividadUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * "Guardados" — lista de publicaciones marcadas con el bookmark del Detalle (Punto 4).
 * <p>
 * No confundir con Favoritos (Punto 10, el corazón de las tarjetas): son dos marcadores
 * independientes sobre la misma publicación. Se llega acá desde el menú de la toolbar del
 * Home, igual que "Mis publicaciones" (Punto 5).
 * <p>
 * {@link PublicacionesGuardadas} (Room) solo guarda ids, no el objeto {@link Publicacion}
 * completo — por eso cargar esta pantalla es en dos pasos: primero los ids guardados, después
 * los objetos completos y actualizados al catálogo (ver {@link PublicacionRepository#obtenerVarias}).
 */
public class GuardadosFragment extends Fragment implements
        PublicacionAdapter.OnPublicacionClickListener,
        PublicacionAdapter.OnFavoritoClickListener {

    private final PublicacionRepository repositorio = PublicacionRepositoryMock.getInstancia();
    private final FavoritoRepository favoritoRepositorio = FavoritoRepositoryMock.getInstancia();

    /**
     * Necesita un {@code Context} para Room, que todavía no existe cuando se inicializan los
     * campos del Fragment; se obtiene recién en {@link #onAttach} (mismo patrón que
     * {@code DetalleFragment}/{@code MisPublicacionesFragment}).
     */
    private PublicacionesGuardadas publicacionesGuardadas;

    private PublicacionesVistas publicacionesVistas;

    private RecyclerView listaGuardados;
    private CircularProgressIndicator progreso;
    private View estadoVacio;

    private PublicacionAdapter adapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        publicacionesGuardadas = PublicacionesGuardadas.getInstancia(context);
        publicacionesVistas = PublicacionesVistas.getInstancia(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guardados, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        listaGuardados = view.findViewById(R.id.listaGuardados);
        progreso = view.findViewById(R.id.progresoGuardados);
        estadoVacio = view.findViewById(R.id.estadoVacioGuardados);

        toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        adapter = new PublicacionAdapter(favoritoRepositorio, this, this);
        listaGuardados.setLayoutManager(new LinearLayoutManager(requireContext()));
        listaGuardados.setAdapter(adapter);

        cargarGuardados();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listaGuardados = null;
        progreso = null;
        estadoVacio = null;
        adapter = null;
    }

    private void cargarGuardados() {
        progreso.setVisibility(View.VISIBLE);
        listaGuardados.setVisibility(View.GONE);
        estadoVacio.setVisibility(View.GONE);

        publicacionesGuardadas.listar(new RepositorioCallback<List<String>>() {
            @Override
            public void onExito(List<String> ids) {
                if (listaGuardados == null) {
                    return; // la vista ya no existe
                }
                repositorio.obtenerVarias(ids, new RepositorioCallback<List<Publicacion>>() {
                    @Override
                    public void onExito(List<Publicacion> guardados) {
                        if (listaGuardados == null) {
                            return;
                        }
                        mostrarGuardados(guardados);
                    }

                    @Override
                    public void onError(String mensaje) {
                        if (listaGuardados == null) {
                            return;
                        }
                        mostrarError(mensaje);
                    }
                });
            }

            @Override
            public void onError(String mensaje) {
                if (listaGuardados == null) {
                    return;
                }
                mostrarError(mensaje);
            }
        });
    }

    private void mostrarGuardados(List<Publicacion> guardados) {
        progreso.setVisibility(View.GONE);
        adapter.reemplazar(guardados);

        boolean sinGuardados = guardados.isEmpty();
        estadoVacio.setVisibility(sinGuardados ? View.VISIBLE : View.GONE);
        listaGuardados.setVisibility(sinGuardados ? View.GONE : View.VISIBLE);
    }

    private void mostrarError(String mensaje) {
        progreso.setVisibility(View.GONE);
        Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG)
                .setAction(R.string.error_reintentar, v -> cargarGuardados())
                .show();
    }

    /** El usuario tocó una publicación del listado: se va a su Detalle, igual que desde el Home. */
    @Override
    public void onPublicacionClick(Publicacion publicacion) {
        publicacionesVistas.registrarVista(publicacion);
        Bundle argumentos = new Bundle();
        argumentos.putString("publicacionId", publicacion.getId());
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_guardados_to_detalle, argumentos);
    }

    /**
     * El usuario tocó el corazón de favorito de una tarjeta — Punto 10, independiente de
     * "guardado". A diferencia de {@code FavoritosFragment}, desmarcar acá NO saca el ítem
     * de la lista: esta pantalla es el conjunto de guardados, no el de favoritos. Mismo
     * criterio que {@code HomeFragment.onFavoritoClick}.
     */
    @Override
    public void onFavoritoClick(Publicacion publicacion, boolean favoritoNuevo) {
        // Marcar/desmarcar favorito requiere conexión.
        if (!ConectividadUtils.hayConexion(requireContext())) {
            if (adapter != null) {
                adapter.refrescarFavorito(publicacion.getId());
            }
            Snackbar.make(requireView(), R.string.error_accion_requiere_conexion, Snackbar.LENGTH_SHORT).show();
            return;
        }

        RepositorioCallback<Void> callback = new RepositorioCallback<Void>() {
            @Override
            public void onExito(Void resultado) {
                // El ícono ya está pintado correctamente desde el click; nada más que hacer.
            }

            @Override
            public void onError(String mensaje) {
                if (listaGuardados == null || adapter == null) {
                    return; // la vista ya no existe
                }
                adapter.refrescarFavorito(publicacion.getId());
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_SHORT).show();
            }
        };

        if (favoritoNuevo) {
            favoritoRepositorio.marcar(publicacion, callback);
        } else {
            favoritoRepositorio.desmarcar(publicacion.getId(), callback);
        }
    }
}
