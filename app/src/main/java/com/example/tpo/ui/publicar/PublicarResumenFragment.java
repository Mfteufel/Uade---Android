package com.example.tpo.ui.publicar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.example.tpo.R;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.BorradorPublicacion;
import com.example.tpo.model.MiPublicacion;
import com.example.tpo.util.FormatoUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

/**
 * Paso 5 (último) del wizard de "Publicar artículo" (Punto 5): resumen final.
 * <p>
 * Es el único paso que efectivamente llama a la API (los cuatro anteriores solo
 * completan el {@link BorradorPublicacion}). Si la publicación se crea con
 * éxito, se vuelve al Home y se borra el borrador; si falla, se muestra el
 * error y el usuario se queda en esta pantalla para reintentar sin perder nada
 * de lo que cargó.
 */
public class PublicarResumenFragment extends PublicarPasoFragment {

    private TextView textoTitulo;
    private TextView textoPrecio;
    private TextView textoFotos;
    private TextView textoCategoria;
    private TextView textoEstado;
    private TextView textoZona;
    private TextView textoDescripcion;
    private CircularProgressIndicator progresoPublicando;
    private MaterialButton botonAtras;
    private MaterialButton botonPublicar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_publicar_resumen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textoTitulo = view.findViewById(R.id.textoTitulo);
        textoPrecio = view.findViewById(R.id.textoPrecio);
        textoFotos = view.findViewById(R.id.textoFotos);
        textoCategoria = view.findViewById(R.id.textoCategoria);
        textoEstado = view.findViewById(R.id.textoEstado);
        textoZona = view.findViewById(R.id.textoZona);
        textoDescripcion = view.findViewById(R.id.textoDescripcion);
        progresoPublicando = view.findViewById(R.id.progresoPublicando);
        botonAtras = view.findViewById(R.id.botonAtras);
        botonPublicar = view.findViewById(R.id.botonPublicar);

        mostrarResumen(viewModel.getBorrador().getValue());

        botonAtras.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        botonPublicar.setOnClickListener(v -> publicar());

        viewModel.getPublicando().observe(getViewLifecycleOwner(), publicando -> {
            progresoPublicando.setVisibility(publicando ? View.VISIBLE : View.GONE);
            botonPublicar.setEnabled(!publicando);
            botonAtras.setEnabled(!publicando);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        textoTitulo = null;
        textoPrecio = null;
        textoFotos = null;
        textoCategoria = null;
        textoEstado = null;
        textoZona = null;
        textoDescripcion = null;
        progresoPublicando = null;
        botonAtras = null;
        botonPublicar = null;
    }

    private void mostrarResumen(@Nullable BorradorPublicacion borrador) {
        if (borrador == null) {
            return;
        }
        textoTitulo.setText(borrador.getTitulo());
        textoPrecio.setText(FormatoUtils.precio(borrador.getPrecio() != null ? borrador.getPrecio() : 0));
        textoFotos.setText(getResources().getQuantityString(
                R.plurals.resumen_fotos, borrador.getFotos().size(), borrador.getFotos().size()));
        if (borrador.getCategoria() != null) {
            textoCategoria.setText(getString(R.string.resumen_categoria, getString(borrador.getCategoria().getEtiqueta())));
        }
        if (borrador.getEstadoArticulo() != null) {
            textoEstado.setText(getString(R.string.resumen_estado, getString(borrador.getEstadoArticulo().getEtiqueta())));
        }
        if (borrador.getZona() != null) {
            textoZona.setText(getString(R.string.resumen_zona, borrador.getZona().getNombre()));
        }
        textoDescripcion.setText(borrador.getDescripcion());
    }

    private void publicar() {
        viewModel.publicar(new RepositorioCallback<MiPublicacion>() {
            @Override
            public void onExito(MiPublicacion resultado) {
                if (getView() == null) {
                    return;
                }
                Snackbar.make(requireView(), R.string.publicar_exito, Snackbar.LENGTH_LONG).show();
                NavHostFragment.findNavController(PublicarResumenFragment.this)
                        .popBackStack(R.id.homeFragment, false);
            }

            @Override
            public void onError(String mensaje) {
                if (getView() == null) {
                    return;
                }
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
