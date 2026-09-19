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
public class LoginFragment extends Fragment {

    public static final String ARG_CERRAR_SESION = "cerrarSesion";
    private static final String TAG = "Login";

    @Inject
    AuthApi authApi;
    @Inject
    TokenManager tokenManager;

    private EditText campoEmail;
    private EditText campoClave;
    private TextView textoError;
    private Button botonIngresar;
    private Button botonCodigo;
    private ProgressBar progreso;

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

        campoEmail = view.findViewById(R.id.campoEmail);
        campoClave = view.findViewById(R.id.campoClave);
        textoError = view.findViewById(R.id.textoError);
        botonIngresar = view.findViewById(R.id.botonIngresar);
        botonCodigo = view.findViewById(R.id.botonCodigo);
        progreso = view.findViewById(R.id.progreso);

        botonIngresar.setOnClickListener(v -> ingresarConClave());
        botonCodigo.setOnClickListener(v -> pedirCodigo());

        boolean cerrarSesion = getArguments() != null
                && getArguments().getBoolean(ARG_CERRAR_SESION, false);
        if (cerrarSesion) {
            tokenManager.clearToken();
        } else if (tokenManager.getToken() != null) {
            recuperarSesion();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        campoEmail = null;
        campoClave = null;
        textoError = null;
        botonIngresar = null;
        botonCodigo = null;
        progreso = null;
    }

    // si ya habia un token guardado se valida contra el servidor y se saltea el login
    private void recuperarSesion() {
        mostrarCargando(true);
        authApi.sesionActual().enqueue(new Callback<SesionResponse.Usuario>() {
            @Override
            public void onResponse(@NonNull Call<SesionResponse.Usuario> call,
                                   @NonNull Response<SesionResponse.Usuario> response) {
                if (getView() == null) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    SesionLocal.actualizar(response.body());
                    irAlHome();
                } else if (response.code() == 401) {
                    tokenManager.clearToken();
                    mostrarCargando(false);
                    mostrarError(getString(R.string.login_sesion_vencida));
                } else {
                    mostrarCargando(false);
                    mostrarError(getString(R.string.login_error_servidor, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<SesionResponse.Usuario> call, @NonNull Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                mostrarError(getString(R.string.login_sin_conexion));
            }
        });
    }

    private void ingresarConClave() {
        String email = campoEmail.getText().toString().trim();
        String clave = campoClave.getText().toString();

        if (email.isEmpty() || clave.isEmpty()) {
            mostrarError(getString(R.string.login_completar_campos));
            return;
        }
        if (!emailValido(email)) {
            mostrarError(getString(R.string.login_email_invalido));
            return;
        }

        mostrarCargando(true);
        authApi.login(new LoginRequest(email, clave)).enqueue(new Callback<SesionResponse>() {
            @Override
            public void onResponse(@NonNull Call<SesionResponse> call,
                                   @NonNull Response<SesionResponse> response) {
                if (getView() == null) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveToken(response.body().getToken());
                    SesionLocal.actualizar(response.body().getUsuario());
                    irAlHome();
                    return;
                }
                mostrarCargando(false);
                if (response.code() == 401) {
                    mostrarError(getString(R.string.login_credenciales_incorrectas));
                } else if (response.code() == 400) {
                    mostrarError(getString(R.string.login_email_invalido));
                } else {
                    mostrarError(getString(R.string.login_error_servidor, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<SesionResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                mostrarError(getString(R.string.login_sin_conexion));
            }
        });
    }

    private void pedirCodigo() {
        String email = campoEmail.getText().toString().trim();

        if (!emailValido(email)) {
            mostrarError(getString(R.string.login_email_para_codigo));
            return;
        }

        mostrarCargando(true);
        authApi.pedirCodigo(new CodigoRequest(email)).enqueue(new Callback<CodigoResponse>() {
            @Override
            public void onResponse(@NonNull Call<CodigoResponse> call,
                                   @NonNull Response<CodigoResponse> response) {
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                if (response.isSuccessful() && response.body() != null) {
                    Bundle argumentos = new Bundle();
                    argumentos.putString(CodigoFragment.ARG_EMAIL, email);
                    argumentos.putString(CodigoFragment.ARG_CODIGO_DE_PRUEBA, response.body().getCodigo());
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_login_to_codigo, argumentos);
                } else if (response.code() == 400) {
                    mostrarError(getString(R.string.login_email_invalido));
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

    // mientras hay una llamada en curso se bloquean los botones: evita el doble click
    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonIngresar.setEnabled(!cargando);
        botonCodigo.setEnabled(!cargando);
        if (cargando) {
            textoError.setVisibility(View.GONE);
        }
    }

    private void mostrarError(String mensaje) {
        textoError.setText(mensaje);
        textoError.setVisibility(View.VISIBLE);
    }

    private void irAlHome() {
        Navigation.findNavController(requireView()).navigate(R.id.action_login_to_home);
    }
}
