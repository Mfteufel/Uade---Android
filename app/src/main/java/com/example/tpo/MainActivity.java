package com.example.tpo;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Única Activity de Ronda (Single Activity Architecture).
 * <p>
 * No tiene lógica de pantalla: solo hospeda el NavHostFragment declarado en
 * activity_main.xml. Cada pantalla de la app es un Fragment y la navegación entre
 * ellas la maneja el NavController sobre nav_graph.xml.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dibuja la app debajo de las barras del sistema (look moderno de Android).
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Como el contenido se dibuja debajo de las barras, hay que compensarlas con
        // padding: sin esto el contenido del Home quedaría tapado por la barra de
        // estado arriba y por la de navegación abajo.
        // Se aplica una sola vez acá, en el contenedor raíz, y lo heredan todos los
        // Fragments; así ninguna pantalla tiene que preocuparse por los insets.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
