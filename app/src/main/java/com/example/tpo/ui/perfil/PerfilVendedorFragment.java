package com.example.tpo.ui.perfil;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.data.FavoritoRepository;
import com.example.tpo.data.FavoritoRepositoryMock;
import com.example.tpo.data.PerfilRepository;
import com.example.tpo.data.PerfilVendedor;
import com.example.tpo.data.PublicacionRepository;
import com.example.tpo.data.PublicacionRepositoryMock;
import com.example.tpo.data.PublicacionesVistas;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.data.SesionUsuario;
import com.example.tpo.model.Calificacion;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Usuario;
import com.example.tpo.model.UsuarioResumen;
import com.example.tpo.ui.home.PublicacionAdapter;
import com.example.tpo.util.ConectividadUtils;
import com.example.tpo.util.FormatoUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Perfil público de un usuario — Puntos 2, 4 y 9 del TPO.
 * <p>
 * Es la única pantalla de perfil público de la app: la abre el Detalle ("ver
 * perfil" del vendedor, Punto 4), el Historial (la contraparte de una
 * operación), una calificación (su autor) y el propio perfil ("ver mi perfil
 * público"). Recibe el id por argumento de navegación.
 * <p>
 * Muestra lo que pide el enunciado para consultar a la otra parte antes de
 * operar: reputación completa, antigüedad, publicaciones activas y las
 * calificaciones recibidas. Los datos del usuario, su reputación y sus
 * calificaciones salen de {@link PerfilRepository} (Punto 2); sus publicaciones
 * siguen saliendo de {@link PublicacionRepository} (Puntos 3/4), reusando el
 * mismo {@link PublicacionAdapter} que el Home.
 */
@AndroidEntryPoint
public class PerfilVendedorFragment extends Fragment
        implements PublicacionAdapter.OnPublicacionClickListener,
        PublicacionAdapter.OnFavoritoClickListener,
        CalificacionAdapter.OnAutorClickListener {

    /** Id del usuario a mostrar. Se llama "vendedorId" por compatibilidad con el Detalle. */
    public static final String ARG_VENDEDOR_ID = "vendedorId";

    private static final int PESTANIA_PUBLICACIONES = 0;
    private static final int PESTANIA_CALIFICACIONES = 1;

    /** Lo inyecta Hilt: la pantalla no sabe si del otro lado hay un mock o Retrofit. */
    @Inject
    PerfilRepository perfilRepositorio;

    // Publicaciones y favoritos son de otros puntos y todavía no pasan por Hilt.
    private final PublicacionRepository publicacionRepositorio = PublicacionRepositoryMock.getInstancia();
    private final FavoritoRepository favoritoRepositorio = FavoritoRepositoryMock.getInstancia();

    private PublicacionesVistas publicacionesVistas;

    private String usuarioId;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        publicacionesVistas = PublicacionesVistas.getInstancia(context);
    }

    // --- Vistas. Son null fuera del rango onCreateView..onDestroyView ---
    private View contenidoPerfil;
    private View avatar;
    private TextView nombrePerfil;
    private TextView miembroDesdePerfil;
    private TextView zonaPerfil;
    private View bloqueReputacion;
    private TabLayout pestanias;
    private RecyclerView lista;
    private CircularProgressIndicator progresoLista;
    private View estadoVacio;
    private TextView textoVacio;
    private CircularProgressIndicator progresoInicial;
    private View estadoError;
    private TextView textoError;

    private PublicacionAdapter publicacionAdapter;
    private CalificacionAdapter calificacionAdapter;

    /** Contenido de cada pestaña. {@code null} mientras se está cargando. */
    @Nullable
    private List<Publicacion> publicaciones;
    @Nullable
    private List<Calificacion> calificaciones;
    /** Mensaje si fallaron las calificaciones (el resto del perfil se sigue mostrando). */
    @Nullable
    private String errorCalificaciones;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usuarioId = requireArguments().getString(ARG_VENDEDOR_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil_vendedor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        contenidoPerfil = view.findViewById(R.id.contenidoPerfil);
        avatar = view.findViewById(R.id.avatarPerfil);
        nombrePerfil = view.findViewById(R.id.nombrePerfil);
        miembroDesdePerfil = view.findViewById(R.id.miembroDesdePerfil);
        zonaPerfil = view.findViewById(R.id.zonaPerfil);
        bloqueReputacion = view.findViewById(R.id.bloqueReputacion);
        pestanias = view.findViewById(R.id.tabsPerfil);
        lista = view.findViewById(R.id.listaPerfil);
        progresoLista = view.findViewById(R.id.progresoLista);
        estadoVacio = view.findViewById(R.id.estadoVacioPerfil);
        textoVacio = view.findViewById(R.id.textoVacioPerfil);
        progresoInicial = view.findViewById(R.id.progresoInicial);
        estadoError = view.findViewById(R.id.estadoError);
        textoError = view.findViewById(R.id.textoError);

        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        if (esMiPerfil()) {
            toolbar.setTitle(R.string.perfil_publico_titulo_propio);
        }
        view.findViewById(R.id.botonReintentar).setOnClickListener(v -> cargarPerfil());

        publicacionAdapter = new PublicacionAdapter(favoritoRepositorio, this, this);
        calificacionAdapter = new CalificacionAdapter(this);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));

        pestanias.addTab(pestanias.newTab());
        pestanias.addTab(pestanias.newTab());
        actualizarTitulosPestanias();
        pestanias.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mostrarPestaniaSeleccionada();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        cargarPerfil();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        contenidoPerfil = null;
        avatar = null;
        nombrePerfil = null;
        miembroDesdePerfil = null;
        zonaPerfil = null;
        bloqueReputacion = null;
        pestanias = null;
        lista = null;
        progresoLista = null;
        estadoVacio = null;
        textoVacio = null;
        progresoInicial = null;
        estadoError = null;
        textoError = null;
        publicacionAdapter = null;
        calificacionAdapter = null;
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    /**
     * Primero el perfil: si falla, no hay nada que mostrar y se ve el estado de
     * error. Recién con el perfil en pantalla se piden las dos listas, cada una
     * por su lado: que falle una no tiene por qué esconder la otra.
     */
    private void cargarPerfil() {
        mostrarCarga();
        publicaciones = null;
        calificaciones = null;
        errorCalificaciones = null;

        perfilRepositorio.obtenerPerfilPublico(usuarioId, new RepositorioCallback<Usuario>() {
            @Override
            public void onExito(Usuario usuario) {
                if (lista == null) {
                    return; // la vista ya se destruyó
                }
                mostrarPerfil(usuario);
                cargarPublicaciones();
                cargarCalificaciones();
            }

            @Override
            public void onError(String mensaje) {
                if (lista == null) {
                    return;
                }
                mostrarError(mensaje);
            }
        });
    }

    private void cargarPublicaciones() {
        publicacionRepositorio.obtenerPerfilVendedor(usuarioId, new RepositorioCallback<PerfilVendedor>() {
            @Override
            public void onExito(PerfilVendedor resultado) {
                if (lista == null) {
                    return;
                }
                publicaciones = resultado.getPublicaciones();
                alCargarPestania();
            }

            @Override
            public void onError(String mensaje) {
                if (lista == null) {
                    return;
                }
                // El mock de publicaciones responde error cuando la persona no tiene
                // ninguna publicación en el catálogo (por ejemplo, alguien que solo
                // compra). Para el perfil público eso es "sin publicaciones activas".
                publicaciones = Collections.emptyList();
                alCargarPestania();
            }
        });
    }

    private void cargarCalificaciones() {
        perfilRepositorio.obtenerCalificacionesRecibidas(usuarioId,
                new RepositorioCallback<List<Calificacion>>() {
                    @Override
                    public void onExito(List<Calificacion> resultado) {
                        if (lista == null) {
                            return;
                        }
                        calificaciones = resultado;
                        alCargarPestania();
                    }

                    @Override
                    public void onError(String mensaje) {
                        if (lista == null) {
                            return;
                        }
                        calificaciones = Collections.emptyList();
                        errorCalificaciones = mensaje;
                        alCargarPestania();
                    }
                });
    }

    // ------------------------------------------------------------------
    // Pintado
    // ------------------------------------------------------------------

    private void mostrarCarga() {
        progresoInicial.setVisibility(View.VISIBLE);
        contenidoPerfil.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
    }

    private void mostrarError(String mensaje) {
        progresoInicial.setVisibility(View.GONE);
        contenidoPerfil.setVisibility(View.GONE);
        estadoError.setVisibility(View.VISIBLE);
        textoError.setText(mensaje);
    }

    private void mostrarPerfil(Usuario usuario) {
        PerfilUi.pintarAvatar(this, avatar, usuario, perfilRepositorio);
        nombrePerfil.setText(usuario.getNombre());
        miembroDesdePerfil.setText(getString(R.string.perfil_antiguedad,
                FormatoUtils.mesYAnio(usuario.getFechaAlta())));
        zonaPerfil.setVisibility(usuario.getZona() == null ? View.GONE : View.VISIBLE);
        if (usuario.getZona() != null) {
            zonaPerfil.setText(getString(R.string.perfil_publico_zona, usuario.getZona().getNombre()));
        }
        PerfilUi.pintarReputacion(bloqueReputacion, usuario.getReputacion(),
                R.string.perfil_publico_sin_calificaciones);

        progresoInicial.setVisibility(View.GONE);
        estadoError.setVisibility(View.GONE);
        contenidoPerfil.setVisibility(View.VISIBLE);
        mostrarPestaniaSeleccionada();
    }

    private void alCargarPestania() {
        actualizarTitulosPestanias();
        mostrarPestaniaSeleccionada();
    }

    /** "Publicaciones (3)" una vez cargadas; sin número mientras cargan. */
    private void actualizarTitulosPestanias() {
        TabLayout.Tab pestaniaPublicaciones = pestanias.getTabAt(PESTANIA_PUBLICACIONES);
        TabLayout.Tab pestaniaCalificaciones = pestanias.getTabAt(PESTANIA_CALIFICACIONES);
        if (pestaniaPublicaciones != null) {
            pestaniaPublicaciones.setText(publicaciones == null
                    ? getString(R.string.perfil_tab_publicaciones)
                    : getString(R.string.perfil_tab_publicaciones_cantidad, publicaciones.size()));
        }
        if (pestaniaCalificaciones != null) {
            pestaniaCalificaciones.setText(calificaciones == null
                    ? getString(R.string.perfil_tab_calificaciones)
                    : getString(R.string.perfil_tab_calificaciones_cantidad, calificaciones.size()));
        }
    }

    /** Muestra en la lista el contenido de la pestaña elegida, o su carga o su vacío. */
    private void mostrarPestaniaSeleccionada() {
        boolean verPublicaciones = pestanias.getSelectedTabPosition() != PESTANIA_CALIFICACIONES;
        List<?> datos = verPublicaciones ? publicaciones : calificaciones;

        if (datos == null) {
            progresoLista.setVisibility(View.VISIBLE);
            lista.setVisibility(View.GONE);
            estadoVacio.setVisibility(View.GONE);
            return;
        }
        progresoLista.setVisibility(View.GONE);

        if (verPublicaciones) {
            if (lista.getAdapter() != publicacionAdapter) {
                lista.setAdapter(publicacionAdapter);
            }
            publicacionAdapter.reemplazar(publicaciones);
            textoVacio.setText(R.string.perfil_publico_sin_publicaciones);
        } else {
            if (lista.getAdapter() != calificacionAdapter) {
                lista.setAdapter(calificacionAdapter);
            }
            calificacionAdapter.reemplazar(calificaciones);
            if (errorCalificaciones != null) {
                textoVacio.setText(errorCalificaciones);
            } else {
                textoVacio.setText(R.string.perfil_publico_sin_calificaciones);
            }
        }

        boolean vacio = datos.isEmpty();
        estadoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        lista.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    private boolean esMiPerfil() {
        return usuarioId != null && usuarioId.equals(SesionUsuario.getInstancia().getUsuarioId());
    }

    // ------------------------------------------------------------------
    // Interacción
    // ------------------------------------------------------------------

    /** El usuario tocó una publicación del listado: se va a su Detalle, igual que desde el Home. */
    @Override
    public void onPublicacionClick(Publicacion publicacion) {
        publicacionesVistas.registrarVista(publicacion);
        Bundle argumentos = new Bundle();
        argumentos.putString("publicacionId", publicacion.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_perfil_to_detalle, argumentos);
    }

    /** El usuario tocó una calificación: se abre el perfil público de quien la dejó. */
    @Override
    public void onAutorClick(UsuarioResumen autor) {
        if (autor.getId().equals(usuarioId)) {
            return; // ya estamos en su perfil
        }
        Bundle argumentos = new Bundle();
        argumentos.putString(ARG_VENDEDOR_ID, autor.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_perfilPublico_to_perfilPublico, argumentos);
    }

    /** El usuario tocó el corazón de una tarjeta — mismo criterio que HomeFragment. */
    @Override
    public void onFavoritoClick(Publicacion publicacion, boolean favoritoNuevo) {
        // Marcar/desmarcar favorito requiere conexión.
        if (!ConectividadUtils.hayConexion(requireContext())) {
            if (publicacionAdapter != null) {
                publicacionAdapter.refrescarFavorito(publicacion.getId());
            }
            Snackbar.make(requireView(), R.string.error_accion_requiere_conexion, Snackbar.LENGTH_SHORT).show();
            return;
        }

        RepositorioCallback<Void> callback = new RepositorioCallback<Void>() {
            @Override
            public void onExito(Void resultado) {
                // El ícono ya está pintado correctamente desde el click; nada más que hacer.
            }

            @Override
            public void onError(String mensaje) {
                if (lista == null || publicacionAdapter == null) {
                    return; // la vista ya no existe
                }
                publicacionAdapter.refrescarFavorito(publicacion.getId());
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_SHORT).show();
            }
        };

        if (favoritoNuevo) {
            favoritoRepositorio.marcar(publicacion, callback);
        } else {
            favoritoRepositorio.desmarcar(publicacion.getId(), callback);
        }
    }
}
