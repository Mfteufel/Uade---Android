package com.example.tpo.ui.mispublicaciones;

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
import com.example.tpo.data.MisPublicacionesRepository;
import com.example.tpo.data.MisPublicacionesRepositoryApi;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.MiPublicacion;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * "Mis publicaciones" (Punto 5): estado de cada publicación propia (activa,
 * pausada, vendida) y acciones para pausar o reactivar.
 * <p>
 * Sigue la misma estructura de estados que {@link com.example.tpo.ui.home.HomeFragment}
 * (lista, vacío, error, carga) para que las dos pantallas se comporten igual
 * ante los mismos casos.
 */
public class MisPublicacionesFragment extends Fragment implements MiPublicacionAdapter.OnAccionClickListener {

    private RecyclerView listaMisPublicaciones;
    private CircularProgressIndicator progresoInicial;
    private View estadoVacio;
    private View estadoError;
    private MiPublicacionAdapter adapter;

    /**
     * {@link MisPublicacionesRepositoryApi} necesita un {@code Context} de
     * aplicación, que todavía no existe cuando se inicializan los campos del
     * Fragment; por eso se crea recién en {@link #onAttach}, que es el primer
     * momento del ciclo de vida en el que hay uno disponible.
     */
    private MisPublicacionesRepository repositorio;

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        repositorio = new MisPublicacionesRepositoryApi(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mis_publicaciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        listaMisPublicaciones = view.findViewById(R.id.listaMisPublicaciones);
        progresoInicial = view.findViewById(R.id.progresoInicial);
        estadoVacio = view.findViewById(R.id.estadoVacio);
        estadoError = view.findViewById(R.id.estadoError);
        View botonReintentar = view.findViewById(R.id.botonReintentar);

        toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        adapter = new MiPublicacionAdapter(this);
        listaMisPublicaciones.setLayoutManager(new LinearLayoutManager(requireContext()));
        listaMisPublicaciones.setAdapter(adapter);

        botonReintentar.setOnClickListener(v -> cargar());

        cargar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listaMisPublicaciones = null;
        progresoInicial = null;
        estadoVacio = null;
        estadoError = null;
        adapter = null;
    }

    private void cargar() {
        if (listaMisPublicaciones == null) {
            return;
        }
        mostrarCargando();
        repositorio.listar(new RepositorioCallback<List<MiPublicacion>>() {
            @Override
            public void onExito(List<MiPublicacion> resultado) {
                if (listaMisPublicaciones == null) {
                    return;
                }
                mostrarResultado(resultado);
            }

            @Override
            public void onError(String mensaje) {
                if (listaMisPublicaciones == null) {
                    return;
                }
                mostrarError();
            }
        });
    }

    private void mostrarCargando() {
        progresoInicial.setVisibility(View.VISIBLE);
        listaMisPublicaciones.setVisibility(View.GONE);
        estadoVacio.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
    }

    private void mostrarResultado(List<MiPublicacion> resultado) {
        progresoInicial.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
        adapter.reemplazar(resultado);
        boolean vacio = resultado.isEmpty();
        estadoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        listaMisPublicaciones.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    private void mostrarError() {
        progresoInicial.setVisibility(View.GONE);
        estadoVacio.setVisibility(View.GONE);
        listaMisPublicaciones.setVisibility(View.GONE);
        estadoError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPausar(MiPublicacion publicacion) {
        cambiarEstado(publicacion, EstadoPublicacion.PAUSADA);
    }

    @Override
    public void onReactivar(MiPublicacion publicacion) {
        cambiarEstado(publicacion, EstadoPublicacion.ACTIVA);
    }

    private void cambiarEstado(MiPublicacion publicacion, EstadoPublicacion nuevoEstado) {
        RepositorioCallback<Void> callback = new RepositorioCallback<Void>() {
            @Override
            public void onExito(Void resultado) {
                if (adapter == null) {
                    return;
                }
                adapter.actualizarEstado(publicacion.getId(), nuevoEstado);
            }

            @Override
            public void onError(String mensaje) {
                if (getView() == null) {
                    return;
                }
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        };
        if (nuevoEstado == EstadoPublicacion.PAUSADA) {
            repositorio.pausar(publicacion.getId(), callback);
        } else {
            repositorio.reactivar(publicacion.getId(), callback);
        }
    }
}
