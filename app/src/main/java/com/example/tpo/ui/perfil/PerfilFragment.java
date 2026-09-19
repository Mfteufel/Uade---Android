package com.example.tpo.ui.perfil;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.tpo.R;
import com.example.tpo.data.PerfilRepository;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.Usuario;
import com.example.tpo.model.Zona;
import com.example.tpo.util.FormatoUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Mi perfil y reputación — Punto 2 del TPO.
 * <p>
 * Muestra y edita los datos personales, cambia la foto de perfil y muestra la
 * reputación. Desde acá se llega al historial de operaciones (Punto 9) y al
 * propio perfil público, para ver lo mismo que ven los demás.
 */
@AndroidEntryPoint
public class PerfilFragment extends Fragment {

    // --- Vistas. Son null fuera del rango onCreateView..onDestroyView ---
    private View contenedorPerfil;
    private CircularProgressIndicator progreso;
    private View estadoError;
    private TextView textoError;

    private View avatar;
    private CircularProgressIndicator progresoFoto;
    private MaterialButton botonCambiarFoto;
    private TextView textoNombre;
    private TextView textoAntiguedad;

    private View bloqueReputacion;
    private TextView textoCalificacionesPendientes;

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

    /** Lo inyecta Hilt: la pantalla no sabe si del otro lado hay un mock o Retrofit. */
    @Inject
    PerfilRepository repositorio;

    /** Último perfil traído del repositorio. Es la referencia para editar y para cancelar. */
    @Nullable
    private Usuario usuarioActual;

    /**
     * Selector de fotos del sistema (Photo Picker). No pide permisos de
     * almacenamiento: el sistema le da acceso a la app solo a la imagen elegida.
     * Se registra como campo porque los launchers tienen que existir antes de que
     * el Fragment llegue a STARTED.
     */
    private final ActivityResultLauncher<PickVisualMediaRequest> selectorFoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::alElegirFoto);

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

        avatar = view.findViewById(R.id.avatarPerfil);
        progresoFoto = view.findViewById(R.id.progresoFoto);
        botonCambiarFoto = view.findViewById(R.id.botonCambiarFoto);
        textoNombre = view.findViewById(R.id.textoNombre);
        textoAntiguedad = view.findViewById(R.id.textoAntiguedad);

        bloqueReputacion = view.findViewById(R.id.bloqueReputacion);
        textoCalificacionesPendientes = view.findViewById(R.id.textoCalificacionesPendientes);

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

        botonCambiarFoto.setOnClickListener(v -> selectorFoto.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        view.findViewById(R.id.botonVerPerfilPublico).setOnClickListener(v -> irAMiPerfilPublico());
        view.findViewById(R.id.botonMisOperaciones).setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.action_miPerfil_to_historial));

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
        avatar = null;
        progresoFoto = null;
        botonCambiarFoto = null;
        textoNombre = null;
        textoAntiguedad = null;
        bloqueReputacion = null;
        textoCalificacionesPendientes = null;
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

        PerfilUi.pintarAvatar(this, avatar, usuario, repositorio);
        textoNombre.setText(usuario.getNombre());
        textoAntiguedad.setText(getString(R.string.perfil_antiguedad,
                FormatoUtils.mesYAnio(usuario.getFechaAlta())));

        PerfilUi.pintarReputacion(bloqueReputacion, usuario.getReputacion(),
                R.string.perfil_sin_calificaciones);

        int pendientes = usuario.getCalificacionesPendientes();
        textoCalificacionesPendientes.setVisibility(pendientes > 0 ? View.VISIBLE : View.GONE);
        textoCalificacionesPendientes.setText(getResources().getQuantityString(
                R.plurals.perfil_calificaciones_pendientes, pendientes, pendientes));

        campoNombre.setText(usuario.getNombre());
        campoEmail.setText(usuario.getEmail());
        campoTelefono.setText(usuario.getTelefono());

        // El segundo parámetro en false evita que el desplegable intente filtrar la
        // lista por el texto que se acaba de poner y termine mostrando una sola opción.
        // La zona puede faltar (usuario recién creado por OTP): queda vacía y la
        // validación pide elegirla al guardar.
        Zona zona = usuario.getZona();
        campoZona.setText(zona == null ? "" : zona.getNombre(), false);

        aplicarModo();
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
    // Foto de perfil
    // ------------------------------------------------------------------

    /** Vuelve del selector. {@code uri} es null si el usuario cerró el selector sin elegir. */
    private void alElegirFoto(@Nullable Uri uri) {
        if (uri == null || contenedorPerfil == null) {
            return;
        }
        progresoFoto.setVisibility(View.VISIBLE);
        botonCambiarFoto.setEnabled(false);

        repositorio.actualizarFoto(uri, new RepositorioCallback<Usuario>() {
            @Override
            public void onExito(Usuario actualizado) {
                if (contenedorPerfil == null) {
                    return;
                }
                progresoFoto.setVisibility(View.GONE);
                botonCambiarFoto.setEnabled(true);
                // Solo se repinta el avatar: si el usuario estaba editando sus
                // datos, lo que venía escribiendo no se pisa.
                usuarioActual = actualizado;
                PerfilUi.pintarAvatar(PerfilFragment.this, avatar, actualizado, repositorio);
                Snackbar.make(requireView(), R.string.perfil_foto_actualizada,
                        Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String mensaje) {
                if (contenedorPerfil == null) {
                    return;
                }
                progresoFoto.setVisibility(View.GONE);
                botonCambiarFoto.setEnabled(true);
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    // ------------------------------------------------------------------
    // Navegación
    // ------------------------------------------------------------------

    /** Abre el propio perfil público: lo que ven los demás, con las calificaciones recibidas. */
    private void irAMiPerfilPublico() {
        if (usuarioActual == null) {
            return;
        }
        Bundle argumentos = new Bundle();
        argumentos.putString(PerfilVendedorFragment.ARG_VENDEDOR_ID, usuarioActual.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_miPerfil_to_perfilPublico, argumentos);
    }

    // ------------------------------------------------------------------
    // Formateo
    // ------------------------------------------------------------------

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
