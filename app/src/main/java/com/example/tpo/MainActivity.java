package com.example.tpo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.tpo.data.BusquedaGuardadaRepositoryApi;
import com.example.tpo.data.FavoritoRepositoryApi;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.login.TokenManager;
import com.example.tpo.model.BusquedaGuardada;
import com.example.tpo.model.Novedad;
import com.example.tpo.model.Publicacion;
import com.example.tpo.service.AvisoDestino;
import com.example.tpo.ui.detalle.DetalleFragment;
import com.example.tpo.ui.home.HomeFragment;
import com.example.tpo.ui.misofertas.MisOfertasFragment;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Única Activity de Ronda (Single Activity Architecture).
 * <p>
 * No tiene lógica de pantalla: solo hospeda el NavHostFragment declarado en
 * activity_main.xml y la bottom nav que lo acompaña. Cada pantalla de la app
 * es un Fragment y la navegación entre ellas la maneja el NavController sobre
 * nav_graph.xml.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    /**
     * Cada cuánto se vuelve a consultar el servidor por novedades de favoritos
     * y búsquedas guardadas mientras la app está en primer plano.
     */
    private static final long INTERVALO_SONDEO_MS = 5_000;

    @Inject
    TokenManager tokenManager;

    private BottomNavigationView bottomNav;
    private NavHostFragment navHostFragment;
    private NavController navController;

    /**
     * Punto 11: a dónde ir porque el usuario tocó un aviso. Queda guardado hasta que se puede
     * navegar de verdad: hay sesión (no se está en login/código/registro) y la Activity está en
     * primer plano. Se resuelve en {@link #abrirAvisoPendiente()}.
     */
    private AvisoDestino destinoPendiente;

    private final Handler handlerSondeo = new Handler(Looper.getMainLooper());
    private final Runnable sondeoNovedades = new Runnable() {
        @Override
        public void run() {
            sondearNovedades();
            handlerSondeo.postDelayed(this, INTERVALO_SONDEO_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dibuja la app debajo de las barras del sistema (look moderno de Android).
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Compensa las barras del sistema con padding en el root. El teclado
        // NO se suma acá (movería la bottom nav en vez de dejar que la tape) —
        // se maneja aparte sobre nav_host_fragment.
        View root = findViewById(R.id.main);
        View navHost = findViewById(R.id.nav_host_fragment);
        bottomNav = findViewById(R.id.bottomNav);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            int solapamiento = teclado.bottom - bottomNav.getHeight() - systemBars.bottom;
            navHost.setPadding(0, 0, 0, Math.max(0, solapamiento));
            return insets;
        });

        // NavigationUI mapea cada <item> de menu_navegacion.xml al <fragment>
        // de nav_graph.xml con el mismo id.
        navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Sin esto, la bottom nav (fija sobre todos los destinos) queda clickeable
        // incluso parado en el login: tocar "Favoritos" o "Perfil" ahí navegaba
        // directo a la app sin haber iniciado sesión.
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean esLogin = esPantallaDeAcceso(destination.getId());
            bottomNav.setVisibility(esLogin ? View.GONE : View.VISIBLE);

            if (destination.getId() == R.id.homeFragment) {
                sondearNovedades();
            }

            // Punto 11: un aviso tocado antes de tener sesión (app cerrada, login o biometría en
            // curso) se abre recién cuando se llega a una pantalla de la app. Con post: no se
            // navega desde adentro del callback de otra navegación.
            if (!esLogin && destinoPendiente != null) {
                bottomNav.post(this::abrirAvisoPendiente);
            }
        });

        actualizarBadgeFavoritos();

        // Si la Activity se recrea (rotación) el aviso ya se procesó la primera vez.
        if (savedInstanceState == null) {
            recibirAviso(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Solo se deja pendiente: con la app en segundo plano, acá el FragmentManager ya guardó su
        // estado y Navigation ignora el navigate() en silencio. Se abre desde onResume, que el
        // sistema llama justo después.
        recibirAviso(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Arranca ya (no espera el primer intervalo) y se reprograma solo.
        handlerSondeo.post(sondeoNovedades);
        abrirAvisoPendiente();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handlerSondeo.removeCallbacks(sondeoNovedades);
    }

    /** Login, código y registro: pantallas sin sesión, donde no hay bottom nav ni a dónde navegar. */
    private static boolean esPantallaDeAcceso(int destinoId) {
        return destinoId == R.id.loginFragment
                || destinoId == R.id.codigoFragment
                || destinoId == R.id.registroFragment;
    }

    /**
     * Punto 11: lee de la notificación a dónde llevar al usuario y lo deja pendiente.
     * <p>
     * Se ignoran los relanzamientos desde "recientes": el sistema vuelve a entregar el Intent
     * original con sus extras, y reabriría un aviso que el usuario ya vio. Los extras se borran
     * una vez leídos por lo mismo: una rotación no tiene que repetir la navegación.
     */
    private void recibirAviso(Intent intent) {
        if (intent == null || (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            return;
        }
        AvisoDestino destino = AvisoDestino.desde(
                intent.getStringExtra(Novedad.EXTRA_TIPO),
                intent.getStringExtra(Novedad.EXTRA_PUBLICACION_ID),
                intent.getStringExtra(Novedad.EXTRA_OFERTA_ID),
                intent.getStringExtra(Novedad.EXTRA_ROL));
        intent.removeExtra(Novedad.EXTRA_TIPO);
        intent.removeExtra(Novedad.EXTRA_PUBLICACION_ID);
        intent.removeExtra(Novedad.EXTRA_OFERTA_ID);
        intent.removeExtra(Novedad.EXTRA_ROL);

        if (destino != null) {
            destinoPendiente = destino;
        }
    }

    /**
     * Abre la publicación u oferta del aviso pendiente, si ya se puede. Si todavía no (sin
     * sesión, en login, o con el estado de los fragments guardado), no hace nada y el aviso
     * sigue pendiente: se reintenta desde onResume y cada vez que se llega a una pantalla de la app.
     * <p>
     * Se navega por id de destino, sin acciones propias: es un salto desde cualquier pantalla.
     */
    private void abrirAvisoPendiente() {
        if (destinoPendiente == null || tokenManager.getToken() == null
                || getSupportFragmentManager().isStateSaved()) {
            return;
        }
        NavDestination actual = navController.getCurrentDestination();
        if (actual == null || esPantallaDeAcceso(actual.getId())) {
            return;
        }

        AvisoDestino destino = destinoPendiente;
        destinoPendiente = null;

        // Una hoja abierta (filtros, oferta, calificar...) no se cierra sola al navegar y
        // quedaría tapando la pantalla del aviso.
        cerrarHojasAbiertas();

        Bundle argumentos = new Bundle();
        if (destino.getTipo() == AvisoDestino.Tipo.OFERTA) {
            argumentos.putString(MisOfertasFragment.ARG_OFERTA_ID, destino.getOfertaId());
            argumentos.putString(MisOfertasFragment.ARG_ROL, destino.esComoVendedor()
                    ? MisOfertasFragment.ROL_VENDEDOR : MisOfertasFragment.ROL_COMPRADOR);
            // popUpTo: si "Mis ofertas" ya estaba abierta se reemplaza, no se apila otra.
            navController.navigate(R.id.misOfertasFragment, argumentos,
                    new NavOptions.Builder().setPopUpTo(R.id.misOfertasFragment, true).build());
        } else {
            argumentos.putString(DetalleFragment.ARG_PUBLICACION_ID, destino.getPublicacionId());
            navController.navigate(R.id.detalleFragment, argumentos);
        }
    }

    /** Cierra los bottom sheets de la pantalla actual y del NavHost. */
    private void cerrarHojasAbiertas() {
        cerrarDialogos(navHostFragment.getChildFragmentManager().getFragments());
        Fragment actual = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
        if (actual != null) {
            cerrarDialogos(actual.getChildFragmentManager().getFragments());
        }
    }

    private void cerrarDialogos(List<Fragment> fragments) {
        for (Fragment fragment : fragments) {
            if (fragment instanceof DialogFragment) {
                ((DialogFragment) fragment).dismissAllowingStateLoss();
            }
        }
    }

    private void sondearNovedades() {
        if (tokenManager.getToken() == null) {
            return;
        }

        FavoritoRepositoryApi.getInstancia(this).listar(new RepositorioCallback<List<Publicacion>>() {
            @Override
            public void onExito(List<Publicacion> resultado) {
                actualizarBadgeFavoritos();
            }

            @Override
            public void onError(String mensaje) {
                // Sin red o el servidor no respondió: se reintenta en el próximo ciclo.
            }
        });

        BusquedaGuardadaRepositoryApi.getInstancia(this).listar(new RepositorioCallback<List<BusquedaGuardada>>() {
            @Override
            public void onExito(List<BusquedaGuardada> resultado) {
                avisarNovedadBusquedasAlHome();
            }

            @Override
            public void onError(String mensaje) {
            }
        });
    }

    private void actualizarBadgeFavoritos() {
        if (FavoritoRepositoryApi.getInstancia(this).hayAlgunaNovedad()) {
            BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.favoritosFragment);
            badge.setBackgroundColor(ContextCompat.getColor(this, R.color.color_indicador_novedad));
        } else {
            bottomNav.removeBadge(R.id.favoritosFragment);
        }
    }

    private void avisarNovedadBusquedasAlHome() {
        Fragment actual = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
        if (actual instanceof HomeFragment) {
            ((HomeFragment) actual).actualizarIndicadorNovedadBusquedas();
        }
    }
}
