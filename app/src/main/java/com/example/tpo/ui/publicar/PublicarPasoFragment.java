package com.example.tpo.ui.publicar;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.fragment.NavHostFragment;

import com.example.tpo.R;

/**
 * Base de los cinco pasos del wizard de "Publicar artículo" (Punto 5).
 * <p>
 * Lo único que comparten los cinco Fragments es cómo consiguen el
 * {@link PublicarArticuloViewModel}: con scope al sub grafo
 * {@code nav_graph_publicar} y no al propio Fragment, para que sobreviva a la
 * navegación entre pasos. Cada paso resuelve su propio layout y su propia
 * lógica de validación; sacar eso a la base sería forzar una abstracción que
 * ningún paso necesita compartir.
 */
public abstract class PublicarPasoFragment extends Fragment {

    protected PublicarArticuloViewModel viewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavBackStackEntry entradaDelGrafo = NavHostFragment.findNavController(this)
                .getBackStackEntry(R.id.nav_graph_publicar);
        viewModel = new ViewModelProvider(
                entradaDelGrafo,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(PublicarArticuloViewModel.class);
    }
}
