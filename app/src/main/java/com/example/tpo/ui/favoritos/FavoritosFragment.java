package com.example.tpo.ui.favoritos;

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
import com.example.tpo.data.FavoritoRepositoryApi;
import com.example.tpo.data.PublicacionesVistas;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.Publicacion;
import com.example.tpo.ui.home.PublicacionAdapter;
import com.example.tpo.util.ConectividadUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * "Mis favoritos" Punto 10
 */
public class FavoritosFragment extends Fragment implements
        PublicacionAdapter.OnPublicacionClickListener,
        PublicacionAdapter.OnFavoritoClickListener {

    private RecyclerView listaFavoritos;
    private CircularProgressIndicator progreso;
    private View estadoVacio;

    private PublicacionAdapter adapter;
    private FavoritoRepository favoritoRepositorio;

    private PublicacionesVistas publicacionesVistas;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        publicacionesVistas = PublicacionesVistas.getInstancia(context);
        favoritoRepositorio = FavoritoRepositoryApi.getInstancia(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favoritos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listaFavoritos = view.findViewById(R.id.listaFavoritos);
        progreso = view.findViewById(R.id.progresoFavoritos);
        estadoVacio = view.findViewById(R.id.estadoVacioFavoritos);

        adapter = new PublicacionAdapter(favoritoRepositorio, this, this);
        listaFavoritos.setLayoutManager(new LinearLayoutManager(requireContext()));
        listaFavoritos.setAdapter(adapter);

        cargarFavoritos();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // El usuario ya vio esta pantalla: se limpian las novedades y su badge.
        favoritoRepositorio.marcarTodoVisto();
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNav);
        bottomNav.removeBadge(R.id.favoritosFragment);

        // Se sueltan las referencias a vistas por lo mismo que en HomeFragment:
        // el Fragment puede sobrevivir a su vista y no hay que retenerlas.
        listaFavoritos = null;
        progreso = null;
        estadoVacio = null;
        adapter = null;
    }

    private void cargarFavoritos() {
        progreso.setVisibility(View.VISIBLE);
        listaFavoritos.setVisibility(View.GONE);
        estadoVacio.setVisibility(View.GONE);

        favoritoRepositorio.listar(new RepositorioCallback<List<Publicacion>>() {
            @Override
            public void onExito(List<Publicacion> favoritos) {
                if (listaFavoritos == null) {
                    return; // la vista ya no existe
                }
                progreso.setVisibility(View.GONE);
                adapter.reemplazar(favoritos);

                boolean sinFavoritos = favoritos.isEmpty();
                estadoVacio.setVisibility(sinFavoritos ? View.VISIBLE : View.GONE);
                listaFavoritos.setVisibility(sinFavoritos ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(String mensaje) {
                if (listaFavoritos == null) {
                    return;
                }
                progreso.setVisibility(View.GONE);
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG)
                        .setAction(R.string.error_reintentar, v -> cargarFavoritos())
                        .show();
            }
        });
    }

    @Override
    public void onPublicacionClick(Publicacion publicacion) {
        publicacionesVistas.registrarVista(publicacion);
        Bundle argumentos = new Bundle();
        argumentos.putString("publicacionId", publicacion.getId());
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_favoritos_to_detalle, argumentos);
    }

    /**
     * Acá, a diferencia del Home, desmarcar un favorito lo tiene que sacar de
     * la lista (esta pantalla ES el conjunto de favoritos). Se recarga todo
     * en vez de sacar solo ese ítem: la lista es chica y no hay paginación.
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
                if (!favoritoNuevo) {
                    cargarFavoritos();
                }
            }

            @Override
            public void onError(String mensaje) {
                if (listaFavoritos == null || adapter == null) {
                    return;
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
