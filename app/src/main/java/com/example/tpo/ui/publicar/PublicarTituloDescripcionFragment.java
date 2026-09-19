package com.example.tpo.ui.publicar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.example.tpo.R;
import com.example.tpo.model.BorradorPublicacion;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Paso 2 del wizard de "Publicar artículo" (Punto 5): título y descripción.
 */
public class PublicarTituloDescripcionFragment extends PublicarPasoFragment {

    private TextInputLayout inputTitulo;
    private TextInputLayout inputDescripcion;
    private TextInputEditText campoTitulo;
    private TextInputEditText campoDescripcion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_publicar_titulo_descripcion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inputTitulo = view.findViewById(R.id.inputTitulo);
        inputDescripcion = view.findViewById(R.id.inputDescripcion);
        campoTitulo = view.findViewById(R.id.campoTitulo);
        campoDescripcion = view.findViewById(R.id.campoDescripcion);
        MaterialButton botonAtras = view.findViewById(R.id.botonAtras);
        MaterialButton botonSiguiente = view.findViewById(R.id.botonSiguiente);

        // El borrador ya está cargado: a este paso solo se llega después del
        // paso 1, que espera a que termine de leerse de Room.
        BorradorPublicacion borrador = viewModel.getBorrador().getValue();
        if (borrador != null) {
            campoTitulo.setText(borrador.getTitulo());
            campoDescripcion.setText(borrador.getDescripcion());
        }

        botonAtras.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        botonSiguiente.setOnClickListener(v -> validarYContinuar());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        inputTitulo = null;
        inputDescripcion = null;
        campoTitulo = null;
        campoDescripcion = null;
    }

    private void validarYContinuar() {
        BorradorPublicacion borrador = viewModel.getBorrador().getValue();
        if (borrador == null) {
            return;
        }

        String titulo = textoDe(campoTitulo);
        String descripcion = textoDe(campoDescripcion);

        boolean valido = true;
        if (titulo.isEmpty()) {
            inputTitulo.setError(getString(R.string.publicar_error_titulo));
            valido = false;
        } else {
            inputTitulo.setError(null);
        }
        if (descripcion.isEmpty()) {
            inputDescripcion.setError(getString(R.string.publicar_error_descripcion));
            valido = false;
        } else {
            inputDescripcion.setError(null);
        }
        if (!valido) {
            return;
        }

        borrador.setTitulo(titulo);
        borrador.setDescripcion(descripcion);
        viewModel.guardarPaso(2);
        NavHostFragment.findNavController(this).navigate(R.id.action_tituloDescripcion_to_categoriaEstado);
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
