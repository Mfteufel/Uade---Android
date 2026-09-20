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
import com.example.tpo.model.BorradorPublicacion;
import com.example.tpo.model.Categoria;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.ui.ChipsUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

/**
 * Paso 3 del wizard de "Publicar artículo" (Punto 5): categoría y estado del artículo.
 * <p>
 * Reutiliza {@link ChipsUtils}, igual que el Home y el bottom sheet de filtros:
 * son chips de selección única armados a partir de los mismos enums del dominio.
 */
public class PublicarCategoriaEstadoFragment extends PublicarPasoFragment {

    private ChipGroup grupoCategorias;
    private ChipGroup grupoEstados;
    private TextView textoErrorCategoria;
    private TextView textoErrorEstado;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_publicar_categoria_estado, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        grupoCategorias = view.findViewById(R.id.grupoCategorias);
        grupoEstados = view.findViewById(R.id.grupoEstados);
        textoErrorCategoria = view.findViewById(R.id.textoErrorCategoria);
        textoErrorEstado = view.findViewById(R.id.textoErrorEstado);
        MaterialButton botonAtras = view.findViewById(R.id.botonAtras);
        MaterialButton botonSiguiente = view.findViewById(R.id.botonSiguiente);

        BorradorPublicacion borrador = viewModel.getBorrador().getValue();
        Categoria categoriaElegida = borrador == null ? null : borrador.getCategoria();
        EstadoArticulo estadoElegido = borrador == null ? null : borrador.getEstadoArticulo();

        for (Categoria categoria : Categoria.values()) {
            ChipsUtils.agregarChip(grupoCategorias, getString(categoria.getEtiqueta()),
                    categoria, categoria == categoriaElegida);
        }
        for (EstadoArticulo estado : EstadoArticulo.values()) {
            ChipsUtils.agregarChip(grupoEstados, getString(estado.getEtiqueta()),
                    estado, estado == estadoElegido);
        }

        botonAtras.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        botonSiguiente.setOnClickListener(v -> validarYContinuar());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        grupoCategorias = null;
        grupoEstados = null;
        textoErrorCategoria = null;
        textoErrorEstado = null;
    }

    private void validarYContinuar() {
        BorradorPublicacion borrador = viewModel.getBorrador().getValue();
        if (borrador == null) {
            return;
        }

        Categoria categoria = (Categoria) ChipsUtils.valorSeleccionado(grupoCategorias);
        EstadoArticulo estado = (EstadoArticulo) ChipsUtils.valorSeleccionado(grupoEstados);

        boolean valido = true;
        textoErrorCategoria.setVisibility(categoria == null ? View.VISIBLE : View.GONE);
        valido &= categoria != null;
        textoErrorEstado.setVisibility(estado == null ? View.VISIBLE : View.GONE);
        valido &= estado != null;
        if (!valido) {
            return;
        }

        borrador.setCategoria(categoria);
        borrador.setEstadoArticulo(estado);
        viewModel.guardarPaso(3);
        NavHostFragment.findNavController(this).navigate(R.id.action_categoriaEstado_to_precioZona);
    }
}
