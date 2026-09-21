package com.example.tpo.login;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.tpo.R;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class RegistroFragment extends Fragment {

    private static final String TAG = "Registro";
    private static final int LARGO_MINIMO_DE_CLAVE = 6;

    @Inject
    AuthApi authApi;

    private EditText campoNombre;
    private EditText campoEmail;
    private EditText campoClave;
    private TextView textoError;
    private Button botonCrear;
    private ProgressBar progreso;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        campoNombre = view.findViewById(R.id.campoNombre);
        campoEmail = view.findViewById(R.id.campoEmail);
        campoClave = view.findViewById(R.id.campoClave);
        textoError = view.findViewById(R.id.textoError);
        botonCrear = view.findViewById(R.id.botonCrear);
        progreso = view.findViewById(R.id.progreso);

        botonCrear.setOnClickListener(v -> crearCuenta());
        view.findViewById(R.id.botonYaTengoCuenta).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        campoNombre = null;
        campoEmail = null;
        campoClave = null;
        textoError = null;
        botonCrear = null;
        progreso = null;
    }

    private void crearCuenta() {
        String nombre = campoNombre.getText().toString().trim();
        String email = campoEmail.getText().toString().trim();
        String clave = campoClave.getText().toString();

        if (nombre.isEmpty() || email.isEmpty() || clave.isEmpty()) {
            mostrarError(getString(R.string.registro_completar_campos));
            return;
        }
        if (!emailValido(email)) {
            mostrarError(getString(R.string.login_email_invalido));
            return;
        }
        if (clave.length() < LARGO_MINIMO_DE_CLAVE) {
            mostrarError(getString(R.string.registro_clave_corta, LARGO_MINIMO_DE_CLAVE));
            return;
        }

        mostrarCargando(true);
        authApi.registrar(new RegistroRequest(nombre, email, clave)).enqueue(new Callback<CodigoResponse>() {
            @Override
            public void onResponse(@NonNull Call<CodigoResponse> call,
                                   @NonNull Response<CodigoResponse> response) {
                if (getView() == null) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    // la cuenta se activa recien al confirmar el codigo que llega por mail
                    mostrarCargando(false);
                    Bundle argumentos = new Bundle();
                    argumentos.putString(CodigoFragment.ARG_EMAIL, email);
                    argumentos.putString(CodigoFragment.ARG_CODIGO_DE_PRUEBA, response.body().getCodigo());
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_registro_to_codigo, argumentos);
                    return;
                }
                mostrarCargando(false);
                if (response.code() == 409) {
                    mostrarError(getString(R.string.registro_email_en_uso));
                } else if (response.code() == 400) {
                    mostrarError(getString(R.string.registro_datos_invalidos));
                } else if (response.code() == 429) {
                    mostrarError(getString(R.string.codigo_esperar));
                } else {
                    mostrarError(getString(R.string.login_error_servidor, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<CodigoResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                mostrarError(getString(R.string.login_sin_conexion));
            }
        });
    }

    private boolean emailValido(String email) {
        int arroba = email.indexOf("@");
        return arroba > 0
                && email.indexOf(".", arroba) > arroba + 1
                && !email.endsWith(".")
                && !email.contains(" ");
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonCrear.setEnabled(!cargando);
        if (cargando) {
            textoError.setVisibility(View.GONE);
        }
    }

    private void mostrarError(String mensaje) {
        textoError.setText(mensaje);
        textoError.setVisibility(View.VISIBLE);
    }
}
