package com.example.tpo.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.tpo.R;
import com.example.tpo.data.SesionUsuario;

/**
 * Punto 1 - Login con usuario y contraseña.
 * Las credenciales están fijas acá hasta que exista el backend.
 */
public class LoginFragment extends Fragment {

    private static final String USUARIO = "walter@uade.edu.ar";
    private static final String CLAVE = "1234";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText campoUsuario = view.findViewById(R.id.campoUsuario);
        EditText campoClave = view.findViewById(R.id.campoClave);
        TextView textoError = view.findViewById(R.id.textoError);
        Button botonIngresar = view.findViewById(R.id.botonIngresar);

        botonIngresar.setOnClickListener(v -> {
            String usuario = campoUsuario.getText().toString().trim();
            String clave = campoClave.getText().toString();

            if (usuario.isEmpty() || clave.isEmpty()) {
                textoError.setText("Completá los dos campos");
                textoError.setVisibility(View.VISIBLE);
                return;
            }

            if (!usuario.equals(USUARIO) || !clave.equals(CLAVE)) {
                textoError.setText("Usuario o contraseña incorrectos");
                textoError.setVisibility(View.VISIBLE);
                return;
            }

            SesionUsuario.getInstancia().setNombre(usuario);
            Navigation.findNavController(view).navigate(R.id.action_login_to_home);
        });
    }
}
