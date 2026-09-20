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
import com.example.tpo.data.BusquedaGuardadaRepositoryMock;
import com.example.tpo.data.FavoritoRepositoryMock;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.data.SesionUsuario;

/**
 * Punto 1 - Login con usuario y contraseña.
 * Las credenciales están fijas acá hasta que exista el backend.
 */
public class LoginFragment extends Fragment {

    private static final String USUARIO = "walter@uade.edu.ar";
    private static final String CLAVE = "1234";

    private static final RepositorioCallback<Void> SIN_RESULTADO = new RepositorioCallback<Void>() {
        @Override
        public void onExito(Void resultado) {
        }

        @Override
        public void onError(String mensaje) {
        }
    };

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

            SesionUsuario sesion = SesionUsuario.getInstancia();
            sesion.setNombre(usuario);
            // idUsuario (catálogo de vendedores v1..v12 del Punto 4) arrancaba
            // hardcodeado en "v1" (Martina G.), así que cualquiera que entrara por
            // este login se veía dueño de sus publicaciones. Lo pisamos acá con
            // usuarioId (el id real de esta sesión, "u0", que no matchea ningún
            // vendedor del catálogo mock) para que el rol en el Detalle sea
            // consistente con quién inició sesión. Reconciliar de verdad los dos
            // catálogos (v1..v12 vs u0..u2) queda pendiente, ver TODO en
            // SesionUsuario.
            sesion.setIdUsuario(sesion.getUsuarioId());

            FavoritoRepositoryMock.getInstancia().precargar(SIN_RESULTADO);
            BusquedaGuardadaRepositoryMock.getInstancia().precargar(SIN_RESULTADO);

            Navigation.findNavController(view).navigate(R.id.action_login_to_home);
        });
    }
}
