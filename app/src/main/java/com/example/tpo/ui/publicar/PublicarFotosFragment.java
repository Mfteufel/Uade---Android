package com.example.tpo.ui.publicar;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.model.BorradorPublicacion;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Paso 1 del wizard de "Publicar artículo" (Punto 5): elegir fotos de la galería.
 * <p>
 * Es también el paso de entrada al wizard, así que además es quien decide si
 * hay que saltar directo a un paso más adelante: si {@link PublicarArticuloViewModel}
 * cargó de Room un borrador con {@code paso > 0}, significa que el usuario
 * había avanzado más y cerró la app a mitad de carga; en ese caso este
 * Fragment navega directo a donde lo dejó en vez de obligarlo a repasar todo
 * de nuevo.
 */
public class PublicarFotosFragment extends PublicarPasoFragment {

    private RecyclerView listaFotos;
    private MaterialButton botonAgregarFotos;
    private MaterialButton botonSiguiente;
    private TextView textoErrorFotos;
    private CircularProgressIndicator progresoBorrador;

    private FotoSeleccionadaAdapter adapter;
    private boolean yaEvaluoElPasoGuardado = false;

    /**
     * Selector de fotos del sistema (Photo Picker). No pide permisos de galería
     * en el manifest: a partir de este contrato Android resuelve el acceso a las
     * fotos elegidas sin necesidad de READ_MEDIA_IMAGES.
     */
    private final ActivityResultLauncher<PickVisualMediaRequest> selectorFotos =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(), uris -> {
                if (uris.isEmpty()) {
                    return;
                }
                BorradorPublicacion borrador = viewModel.getBorrador().getValue();
                if (borrador == null) {
                    return;
                }
                List<Uri> fotos = new ArrayList<>(borrador.getFotos());
                fotos.addAll(uris);
                borrador.setFotos(fotos);
                mostrarFotos(fotos);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_publicar_fotos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listaFotos = view.findViewById(R.id.listaFotos);
        botonAgregarFotos = view.findViewById(R.id.botonAgregarFotos);
        botonSiguiente = view.findViewById(R.id.botonSiguiente);
        textoErrorFotos = view.findViewById(R.id.textoErrorFotos);
        progresoBorrador = view.findViewById(R.id.progresoBorrador);

        adapter = new FotoSeleccionadaAdapter(this::quitarFoto);
        listaFotos.setAdapter(adapter);

        botonAgregarFotos.setOnClickListener(v -> selectorFotos.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        botonSiguiente.setOnClickListener(v -> validarYContinuar());

        viewModel.getCargandoBorrador().observe(getViewLifecycleOwner(), cargando -> {
            progresoBorrador.setVisibility(cargando ? View.VISIBLE : View.GONE);
            int visibilidad = cargando ? View.GONE : View.VISIBLE;
            botonAgregarFotos.setVisibility(visibilidad);
            botonSiguiente.setVisibility(visibilidad);
            if (!cargando) {
                mostrarFotos(viewModel.getBorrador().getValue().getFotos());
                saltarAlPasoGuardadoSiCorresponde();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listaFotos = null;
        botonAgregarFotos = null;
        botonSiguiente = null;
        textoErrorFotos = null;
        progresoBorrador = null;
        adapter = null;
    }

    private void quitarFoto(int posicion) {
        BorradorPublicacion borrador = viewModel.getBorrador().getValue();
        if (borrador == null) {
            return;
        }
        List<Uri> fotos = new ArrayList<>(borrador.getFotos());
        fotos.remove(posicion);
        borrador.setFotos(fotos);
        mostrarFotos(fotos);
    }

    private void mostrarFotos(List<Uri> fotos) {
        adapter.actualizar(fotos);
        listaFotos.setVisibility(fotos.isEmpty() ? View.GONE : View.VISIBLE);
        if (!fotos.isEmpty()) {
            textoErrorFotos.setVisibility(View.GONE);
        }
    }

    /**
     * Si el borrador recién cargado quedó en un paso más adelante, salta directo
     * ahí. Se ejecuta una sola vez por instancia del Fragment: sin la guarda, cada
     * vez que {@code getCargandoBorrador()} volviera a emitir se repetiría la
     * navegación.
     */
    private void saltarAlPasoGuardadoSiCorresponde() {
        if (yaEvaluoElPasoGuardado) {
            return;
        }
        yaEvaluoElPasoGuardado = true;

        BorradorPublicacion borrador = viewModel.getBorrador().getValue();
        if (borrador == null || borrador.getPaso() <= 0) {
            return;
        }
        NavHostFragment.findNavController(this).navigate(destinoDelPaso(borrador.getPaso()));
    }

    private static int destinoDelPaso(int paso) {
        switch (paso) {
            case 1:
                return R.id.publicarTituloDescripcionFragment;
            case 2:
                return R.id.publicarCategoriaEstadoFragment;
            case 3:
                return R.id.publicarPrecioZonaFragment;
            case 4:
                return R.id.publicarResumenFragment;
            default:
                return R.id.publicarFotosFragment;
        }
    }

    private void validarYContinuar() {
        BorradorPublicacion borrador = viewModel.getBorrador().getValue();
        if (borrador == null || borrador.getFotos().isEmpty()) {
            textoErrorFotos.setVisibility(View.VISIBLE);
            return;
        }
        viewModel.guardarPaso(1);
        NavHostFragment.findNavController(this).navigate(R.id.action_fotos_to_tituloDescripcion);
    }
}
