package com.example.tpo.ui.perfil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tpo.R;
import com.example.tpo.data.PerfilRepository;
import com.example.tpo.data.PerfilRepositoryMock;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.Reputacion;
import com.example.tpo.model.Usuario;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.example.tpo.model.Zona;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.button.MaterialButton;

import android.text.TextUtils;
import com.google.android.material.snackbar.Snackbar;

/**
 * Mi perfil y reputación — Punto 2 del TPO.
 */
public class PerfilFragment extends Fragment {

    // --- Vistas. Son null fuera del rango onCreateView..onDestroyView ---
    private View contenedorPerfil;
    private CircularProgressIndicator progreso;
    private View estadoError;
    private TextView textoError;

    private TextView textoInicial;
    private TextView textoNombre;
    private TextView textoAntiguedad;

    private View contenedorPromedio;
    private RatingBar barraEstrellas;
    private TextView textoPromedio;
    private TextView textoSinCalificaciones;
    private TextView textoOperacionesVendedor;
    private TextView textoOperacionesComprador;

    private TextInputEditText campoNombre;
    private TextInputEditText campoEmail;
    private TextInputEditText campoTelefono;

    private AutoCompleteTextView campoZona;

    private MaterialButton botonEditar;
    private View contenedorBotonesEdicion;
    private MaterialButton botonGuardar;
    private MaterialButton botonCancelar;

    /** true mientras la pantalla está en modo edición. */
    private boolean editando = false;

    private final PerfilRepository repositorio = PerfilRepositoryMock.getInstancia();

    /** Último perfil traído del repositorio. Es la referencia para editar y para cancelar. */
    @Nullable
    private Usuario usuarioActual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Acá solo se infla el layout. Los findViewById y los listeners van en
        // onViewCreated, cuando la vista ya existe.
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        contenedorPerfil = view.findViewById(R.id.contenedorPerfil);
        progreso = view.findViewById(R.id.progresoPerfil);
        estadoError = view.findViewById(R.id.estadoErrorPerfil);
        textoError = view.findViewById(R.id.textoErrorPerfil);

        textoInicial = view.findViewById(R.id.textoInicial);
        textoNombre = view.findViewById(R.id.textoNombre);
        textoAntiguedad = view.findViewById(R.id.textoAntiguedad);

        contenedorPromedio = view.findViewById(R.id.contenedorPromedio);
        barraEstrellas = view.findViewById(R.id.barraEstrellas);
        textoPromedio = view.findViewById(R.id.textoPromedio);
        textoSinCalificaciones = view.findViewById(R.id.textoSinCalificaciones);
        textoOperacionesVendedor = view.findViewById(R.id.textoOperacionesVendedor);
        textoOperacionesComprador = view.findViewById(R.id.textoOperacionesComprador);

        campoNombre = view.findViewById(R.id.campoNombre);
        campoEmail = view.findViewById(R.id.campoEmail);
        campoTelefono = view.findViewById(R.id.campoTelefono);
        campoZona = view.findViewById(R.id.campoZona);
        configurarDesplegableZonas();

        botonEditar = view.findViewById(R.id.botonEditar);
        contenedorBotonesEdicion = view.findViewById(R.id.contenedorBotonesEdicion);
        botonGuardar = view.findViewById(R.id.botonGuardar);
        botonCancelar = view.findViewById(R.id.botonCancelar);

        botonEditar.setOnClickListener(v -> entrarEnEdicion());
        botonCancelar.setOnClickListener(v -> cancelarEdicion());

        botonGuardar.setOnClickListener(v -> guardarCambios());

        view.findViewById(R.id.botonReintentarPerfil)
                .setOnClickListener(v -> cargarPerfil());

        cargarPerfil();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // El Fragment puede seguir vivo después de que su vista muere; si guardara
        // las referencias, mantendría en memoria todo el árbol de vistas.
        contenedorPerfil = null;
        progreso = null;
        estadoError = null;
        textoError = null;
        textoInicial = null;
        textoNombre = null;
        textoAntiguedad = null;
        contenedorPromedio = null;
        barraEstrellas = null;
        textoPromedio = null;
        textoSinCalificaciones = null;
        textoOperacionesVendedor = null;
        textoOperacionesComprador = null;
        campoNombre = null;
        campoEmail = null;
        campoTelefono = null;
        campoZona = null;
        botonEditar = null;
        contenedorBotonesEdicion = null;
        botonGuardar = null;
        botonCancelar = null;
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    private void cargarPerfil() {
        mostrarCarga();

        repositorio.obtenerMiPerfil(new RepositorioCallback<Usuario>() {
            @Override
            public void onExito(Usuario usuario) {
                // La respuesta puede llegar después de que el usuario salió de la
                // pantalla. Sin este chequeo sería un NullPointerException.
                if (contenedorPerfil == null) {
                    return;
                }
                usuarioActual = usuario;
                mostrarPerfil(usuario);
            }

            @Override
            public void onError(String mensaje) {
                if (contenedorPerfil == null) {
                    return;
                }
                mostrarError(mensaje);
            }
        });
    }

    // ------------------------------------------------------------------
    // Pintado
    // ------------------------------------------------------------------

    private void mostrarPerfil(Usuario usuario) {
        progreso.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
        contenedorPerfil.setVisibility(View.VISIBLE);

        textoInicial.setText(inicialDe(usuario.getNombre()));
        textoNombre.setText(usuario.getNombre());
        textoAntiguedad.setText(getString(R.string.perfil_antiguedad,
                mesYAnioDe(usuario.getFechaAlta())));

        mostrarReputacion(usuario.getReputacion());

        campoNombre.setText(usuario.getNombre());
        campoEmail.setText(usuario.getEmail());
        campoTelefono.setText(usuario.getTelefono());

        // El segundo parámetro en false evita que el desplegable intente filtrar la
// lista por el texto que se acaba de poner y termine mostrando una sola opción.
        campoZona.setText(usuario.getZona().getNombre(), false);


        aplicarModo();
    }

    private void mostrarReputacion(Reputacion reputacion) {
        // Un usuario sin historial y uno con mala reputación darían el mismo 0,0.
        // Se distinguen mostrando bloques distintos.
        boolean hayCalificaciones = reputacion.tieneCalificaciones();

        contenedorPromedio.setVisibility(hayCalificaciones ? View.VISIBLE : View.GONE);
        textoSinCalificaciones.setVisibility(hayCalificaciones ? View.GONE : View.VISIBLE);

        if (hayCalificaciones) {
            barraEstrellas.setRating((float) reputacion.getPromedioEstrellas());
            textoPromedio.setText(String.format(Locale.forLanguageTag("es-AR"),
                    "%.1f", reputacion.getPromedioEstrellas()));
        }

        textoOperacionesVendedor.setText(
                String.valueOf(reputacion.getOperacionesComoVendedor()));
        textoOperacionesComprador.setText(
                String.valueOf(reputacion.getOperacionesComoComprador()));
    }

    private void mostrarCarga() {
        progreso.setVisibility(View.VISIBLE);
        contenedorPerfil.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
    }

    private void mostrarError(String mensaje) {
        progreso.setVisibility(View.GONE);
        contenedorPerfil.setVisibility(View.GONE);
        estadoError.setVisibility(View.VISIBLE);
        textoError.setText(mensaje);
    }

    // ------------------------------------------------------------------
    // Formateo
    // ------------------------------------------------------------------

    /** Primera letra del nombre, en mayúscula, para el avatar. */
    private String inicialDe(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "?";
        }
        return nombre.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    /**
     * Fecha de alta como "julio de 2025".
     * <p>
     * No se usa FormatoUtils.antiguedad() a propósito: ese método devuelve tiempo
     * relativo ("hace 3 días"), que sirve para una publicación recién subida pero
     * no para una antigüedad de dos años. Acá interesa desde cuándo, no hace cuánto.
     */
    private String mesYAnioDe(long fechaMillis) {
        SimpleDateFormat formato =
                new SimpleDateFormat("MMMM 'de' yyyy", Locale.forLanguageTag("es-AR"));
        return formato.format(new Date(fechaMillis));
    }

    /**
     * Carga las 16 zonas en el desplegable.
     * <p>
     * Se hace una sola vez al crear la vista y no en cada pintado: la lista de zonas
     * no cambia, es un enum.
     */
    private void configurarDesplegableZonas() {
        List<String> nombres = new ArrayList<>();
        for (Zona zona : Zona.values()) {
            nombres.add(zona.getNombre());
        }
        campoZona.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, nombres));
    }

    /**
     * Devuelve la zona cuyo nombre visible coincide con el texto del desplegable.
     * <p>
     * El AutoCompleteTextView guarda texto, no el enum. Al leerlo hay que traducir
     * de vuelta. Devuelve null si no coincide con ninguna, que el repositorio
     * rechaza como dato inválido.
     */
    @Nullable
    private Zona zonaSeleccionada() {
        String texto = campoZona.getText().toString();
        for (Zona zona : Zona.values()) {
            if (zona.getNombre().equals(texto)) {
                return zona;
            }
        }
        return null;
    }
    // ------------------------------------------------------------------
// Modo lectura / edición
// ------------------------------------------------------------------

    private void entrarEnEdicion() {
        editando = true;
        aplicarModo();
        // El foco en el primer campo abre el teclado directamente: el usuario tocó
        // "Editar" porque quiere escribir, no hace falta un toque más.
        campoNombre.requestFocus();
    }

    /**
     * Descarta lo escrito y vuelve a los valores del último perfil cargado.
     * <p>
     * Por eso se guarda {@link #usuarioActual}: sin esa referencia no habría forma
     * de saber qué había antes de que el usuario empezara a tipear.
     */
    private void cancelarEdicion() {
        editando = false;
        if (usuarioActual != null) {
            mostrarPerfil(usuarioActual);
        }
        aplicarModo();
    }

    /**
     * Refleja el modo actual en la pantalla.
     * <p>
     * Está en un solo método y no repartido entre entrar y cancelar: así no hay
     * forma de que un camino habilite los campos y se olvide de cambiar los botones.
     */
    private void aplicarModo() {
        campoNombre.setEnabled(editando);
        campoEmail.setEnabled(editando);
        campoTelefono.setEnabled(editando);
        campoZona.setEnabled(editando);

        botonEditar.setVisibility(editando ? View.GONE : View.VISIBLE);
        contenedorBotonesEdicion.setVisibility(editando ? View.VISIBLE : View.GONE);
    }

    // ------------------------------------------------------------------
// Guardado
// ------------------------------------------------------------------

    private void guardarCambios() {
        if (usuarioActual == null) {
            return;
        }

        Zona zona = zonaSeleccionada();
        if (zona == null) {
            // El desplegable quedó con un texto que no corresponde a ninguna zona.
            // No se manda al repositorio: no hay nada que validar del otro lado.
            Snackbar.make(requireView(), R.string.perfil_zona_invalida,
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        Usuario editado = usuarioActual.conDatosPersonales(
                textoDe(campoNombre),
                textoDe(campoEmail),
                textoDe(campoTelefono),
                zona);

        // Se bloquean los botones mientras viaja la consulta: sin esto, tocar
        // "Guardar" dos veces rápido mandaría dos actualizaciones.
        habilitarBotonesEdicion(false);

        repositorio.actualizarMiPerfil(editado, new RepositorioCallback<Usuario>() {
            @Override
            public void onExito(Usuario guardado) {
                if (contenedorPerfil == null) {
                    return;
                }
                habilitarBotonesEdicion(true);

                // Se pinta lo que devolvió el repositorio, no lo que el usuario
                // escribió: el servidor puede haber normalizado algo (espacios,
                // mayúsculas del email) y la pantalla tiene que mostrar lo guardado.
                usuarioActual = guardado;
                editando = false;
                mostrarPerfil(guardado);

                Snackbar.make(requireView(), R.string.perfil_guardado,
                        Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String mensaje) {
                if (contenedorPerfil == null) {
                    return;
                }
                habilitarBotonesEdicion(true);

                // El error de guardado no ocupa la pantalla: el usuario sigue en modo
                // edición con lo que escribió intacto, para poder corregirlo.
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void habilitarBotonesEdicion(boolean habilitados) {
        botonGuardar.setEnabled(habilitados);
        botonCancelar.setEnabled(habilitados);
    }

    /** Texto del campo, sin espacios sobrantes y nunca null. */
    private String textoDe(TextInputEditText campo) {
        CharSequence texto = campo.getText();
        return TextUtils.isEmpty(texto) ? "" : texto.toString().trim();
    }

}