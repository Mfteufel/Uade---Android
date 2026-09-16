package com.example.tpo;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.tpo.data.FavoritoRepositoryMock;
import com.example.tpo.debug.SimulacionNovedadesReceiver;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Única Activity de Ronda (Single Activity Architecture).
 * <p>
 * No tiene lógica de pantalla: solo hospeda el NavHostFragment declarado en
 * activity_main.xml y la bottom nav que lo acompaña. Cada pantalla de la app
 * es un Fragment y la navegación entre ellas la maneja el NavController sobre
 * nav_graph.xml.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private SimulacionNovedadesReceiver receptorNovedades;

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
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);

        // RECEIVER_EXPORTED porque el broadcast de prueba llega desde `adb shell am
        // broadcast`, que corre como shell y no como este mismo paquete.
        receptorNovedades = new SimulacionNovedadesReceiver(this::actualizarBadgeFavoritos);
        ContextCompat.registerReceiver(this, receptorNovedades, SimulacionNovedadesReceiver.crearFiltro(),
                ContextCompat.RECEIVER_EXPORTED);
        actualizarBadgeFavoritos();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receptorNovedades);
    }

    private void actualizarBadgeFavoritos() {
        if (FavoritoRepositoryMock.getInstancia().hayAlgunaNovedad()) {
            bottomNav.getOrCreateBadge(R.id.favoritosFragment);
        } else {
            bottomNav.removeBadge(R.id.favoritosFragment);
        }
    }
}
