package com.example.tpo.ui.publicar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.model.BorradorPublicacion;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

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
     * Dos {@code ActivityResultLauncher} separados (Clase 6): pedir el permiso
     * y elegir la foto son dos acciones distintas, no se puede lanzar la
     * segunda si la primera no se concedió. Se declara primero
     * {@code elegirFoto} porque {@code pedirPermisoGaleria} la referencia en su
     * callback (Java no permite la referencia inversa entre inicializadores de
     * campo, aunque esté dentro de un lambda que recién corre después).
     */

    /** Abre la galería del sistema y devuelve la {@code Uri} elegida (una por invocación). */
    private final ActivityResultLauncher<String> elegirFoto =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return; // el usuario canceló el selector sin elegir nada
                }
                BorradorPublicacion borrador = viewModel.getBorrador().getValue();
                if (borrador == null) {
                    return;
                }
                // El permiso que da el selector es de esta sesión; como la foto
                // se guarda en el borrador de Room para poder retomarlo después
                // de cerrar la app, hay que pedir que persista más allá de eso.
                try {
                    requireContext().getContentResolver()
                            .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException excepcion) {
                    // Algunos orígenes no soportan permiso persistente; la foto
                    // igual se puede mostrar en esta misma sesión.
                }
                List<Uri> fotos = new ArrayList<>(borrador.getFotos());
                fotos.add(uri);
                borrador.setFotos(fotos);
                mostrarFotos(fotos);
            });

    /** Pide el permiso de galería en runtime; declararlo en el Manifest no alcanza. */
    private final ActivityResultLauncher<String> pedirPermisoGaleria =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    elegirFoto.launch("image/*");
                } else if (textoErrorFotos != null) {
                    Snackbar.make(requireView(), R.string.publicar_fotos_permiso_necesario,
                            Snackbar.LENGTH_LONG).show();
                }
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

        botonAgregarFotos.setOnClickListener(v -> agregarFoto());

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

    /**
     * Punto de entrada de "Agregar fotos": si el permiso de galería ya está
     * concedido, abre el selector directo; si no, lo pide primero (Clase 6 —
     * nunca alcanza con haberlo declarado en el Manifest).
     */
    private void agregarFoto() {
        String permiso = permisoDeGaleria();
        if (ContextCompat.checkSelfPermission(requireContext(), permiso)
                == PackageManager.PERMISSION_GRANTED) {
            elegirFoto.launch("image/*");
        } else {
            pedirPermisoGaleria.launch(permiso);
        }
    }

    /** READ_MEDIA_IMAGES reemplazó a READ_EXTERNAL_STORAGE recién en API 33. */
    private static String permisoDeGaleria() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
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
