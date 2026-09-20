package com.example.tpo.ui.publicar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.example.tpo.R;
import com.example.tpo.model.BorradorPublicacion;
import com.example.tpo.model.Zona;
import com.example.tpo.util.MapaUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Paso 4 del wizard de "Publicar artículo" (Punto 5): precio, zona y dirección
 * de entrega (Punto 8, agregado sobre este mismo paso en vez de uno nuevo).
 */
public class PublicarPrecioZonaFragment extends PublicarPasoFragment {

    private TextInputLayout inputPrecio;
    private TextInputEditText campoPrecio;
    private AutoCompleteTextView campoZona;
    private TextView textoErrorZona;
    private TextInputEditText campoDireccion;
    private TextView textoErrorDireccion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_publicar_precio_zona, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inputPrecio = view.findViewById(R.id.inputPrecio);
        campoPrecio = view.findViewById(R.id.campoPrecio);
        campoZona = view.findViewById(R.id.campoZona);
        textoErrorZona = view.findViewById(R.id.textoErrorZona);
        campoDireccion = view.findViewById(R.id.campoDireccion);
        textoErrorDireccion = view.findViewById(R.id.textoErrorDireccion);
        MaterialButton botonAtras = view.findViewById(R.id.botonAtras);
        MaterialButton botonSiguiente = view.findViewById(R.id.botonSiguiente);

        // setText(nombre, false): el "false" evita que se dispare un filtrado del
        // dropdown por el texto precargado, que con un ArrayAdapter simple
        // terminaría ocultando el resto de las zonas hasta que el usuario borre.
        ArrayAdapter<String> adapterZonas = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, nombresDeZonas());
        campoZona.setAdapter(adapterZonas);

        BorradorPublicacion borrador = viewModel.getBorrador().getValue();
        if (borrador != null) {
            if (borrador.getPrecio() != null) {
                campoPrecio.setText(String.valueOf(borrador.getPrecio().longValue()));
            }
            if (borrador.getZona() != null) {
                campoZona.setText(borrador.getZona().getNombre(), false);
            }
            if (borrador.getDireccionEntrega() != null) {
                campoDireccion.setText(borrador.getDireccionEntrega());
            }
        }

        botonAtras.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        botonSiguiente.setOnClickListener(v -> validarYContinuar());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        inputPrecio = null;
        campoPrecio = null;
        campoZona = null;
        textoErrorZona = null;
        campoDireccion = null;
        textoErrorDireccion = null;
    }

    private static String[] nombresDeZonas() {
        Zona[] zonas = Zona.values();
        String[] nombres = new String[zonas.length];
        for (int i = 0; i < zonas.length; i++) {
            nombres[i] = zonas[i].getNombre();
        }
        return nombres;
    }

    @Nullable
    private static Zona zonaPorNombre(String nombre) {
        for (Zona zona : Zona.values()) {
            if (zona.getNombre().equals(nombre)) {
                return zona;
            }
        }
        return null;
    }

    private void validarYContinuar() {
        BorradorPublicacion borrador = viewModel.getBorrador().getValue();
        if (borrador == null) {
            return;
        }

        Double precio = leerPrecio();
        Zona zona = zonaPorNombre(campoZona.getText().toString().trim());
        String direccion = leerDireccion();

        boolean valido = true;
        if (precio == null || precio <= 0) {
            inputPrecio.setError(getString(R.string.publicar_error_precio));
            valido = false;
        } else {
            inputPrecio.setError(null);
        }
        textoErrorZona.setVisibility(zona == null ? View.VISIBLE : View.GONE);
        valido &= zona != null;
        boolean hayDireccion = MapaUtils.tieneDireccion(direccion);
        textoErrorDireccion.setVisibility(hayDireccion ? View.GONE : View.VISIBLE);
        valido &= hayDireccion;
        if (!valido) {
            return;
        }

        borrador.setPrecio(precio);
        borrador.setZona(zona);
        borrador.setDireccionEntrega(direccion);
        viewModel.guardarPaso(4);
        NavHostFragment.findNavController(this).navigate(R.id.action_precioZona_to_resumen);
    }

    @Nullable
    private Double leerPrecio() {
        CharSequence contenido = campoPrecio.getText();
        String texto = contenido == null ? "" : contenido.toString().trim();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException excepcion) {
            return null;
        }
    }

    @Nullable
    private String leerDireccion() {
        CharSequence contenido = campoDireccion.getText();
        String texto = contenido == null ? "" : contenido.toString().trim();
        return texto.isEmpty() ? null : texto;
    }
}
