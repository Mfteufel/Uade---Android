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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.tpo.R;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class CodigoFragment extends Fragment {

    public static final String ARG_EMAIL = "email";
    public static final String ARG_CODIGO_DE_PRUEBA = "codigoDePrueba";
    private static final String TAG = "Codigo";

    @Inject
    AuthApi authApi;
    @Inject
    TokenManager tokenManager;

    private String email;

    private EditText campoCodigo;
    private TextView textoCodigoDePrueba;
    private TextView textoMensaje;
    private Button botonVerificar;
    private Button botonReenviar;
    private ProgressBar progreso;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_codigo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        email = requireArguments().getString(ARG_EMAIL, "");

        campoCodigo = view.findViewById(R.id.campoCodigo);
        textoCodigoDePrueba = view.findViewById(R.id.textoCodigoDePrueba);
        textoMensaje = view.findViewById(R.id.textoMensaje);
        botonVerificar = view.findViewById(R.id.botonVerificar);
        botonReenviar = view.findViewById(R.id.botonReenviar);
        progreso = view.findViewById(R.id.progreso);

        TextView textoEnviado = view.findViewById(R.id.textoEnviado);
        textoEnviado.setText(getString(R.string.codigo_enviado_a, email));
        mostrarCodigoDePrueba(requireArguments().getString(ARG_CODIGO_DE_PRUEBA));

        botonVerificar.setOnClickListener(v -> verificar());
        botonReenviar.setOnClickListener(v -> reenviar());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        campoCodigo = null;
        textoCodigoDePrueba = null;
        textoMensaje = null;
        botonVerificar = null;
        botonReenviar = null;
        progreso = null;
    }

    private void verificar() {
        String codigo = campoCodigo.getText().toString().trim();
        if (codigo.length() != 6) {
            mostrarMensaje(getString(R.string.codigo_incompleto), true);
            return;
        }

        mostrarCargando(true);
        authApi.verificarCodigo(new CodigoRequest(email, codigo)).enqueue(new Callback<SesionResponse>() {
            @Override
            public void onResponse(@NonNull Call<SesionResponse> call,
                                   @NonNull Response<SesionResponse> response) {
                if (getView() == null) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveToken(response.body().getToken());
                    SesionLocal.actualizar(requireContext(), response.body().getUsuario());
                    Biometria.ofrecerActivar(CodigoFragment.this, tokenManager, () -> {
                        if (getView() != null) {
                            Navigation.findNavController(requireView()).navigate(R.id.action_codigo_to_home);
                        }
                    });
                    return;
                }
                mostrarCargando(false);
                if (response.code() == 401) {
                    mostrarMensaje(getString(R.string.codigo_incorrecto), true);
                } else {
                    mostrarMensaje(getString(R.string.login_error_servidor, response.code()), true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<SesionResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                mostrarMensaje(getString(R.string.login_sin_conexion), true);
            }
        });
    }

    private void reenviar() {
        mostrarCargando(true);
        authApi.reenviarCodigo(new CodigoRequest(email)).enqueue(new Callback<CodigoResponse>() {
            @Override
            public void onResponse(@NonNull Call<CodigoResponse> call,
                                   @NonNull Response<CodigoResponse> response) {
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                if (response.isSuccessful() && response.body() != null) {
                    campoCodigo.setText("");
                    mostrarCodigoDePrueba(response.body().getCodigo());
                    mostrarMensaje(getString(R.string.codigo_reenviado), false);
                } else if (response.code() == 429) {
                    mostrarMensaje(getString(R.string.codigo_esperar), true);
                } else if (response.code() == 404) {
                    mostrarMensaje(getString(R.string.codigo_sin_pedido), true);
                } else {
                    mostrarMensaje(getString(R.string.login_error_servidor, response.code()), true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CodigoResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                mostrarMensaje(getString(R.string.login_sin_conexion), true);
            }
        });
    }

    // el servidor devuelve el codigo solo mientras no hay envio de mail real
    private void mostrarCodigoDePrueba(@Nullable String codigo) {
        if (codigo == null) {
            textoCodigoDePrueba.setVisibility(View.GONE);
        } else {
            textoCodigoDePrueba.setText(getString(R.string.codigo_de_prueba, codigo));
            textoCodigoDePrueba.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonVerificar.setEnabled(!cargando);
        botonReenviar.setEnabled(!cargando);
        if (cargando) {
            textoMensaje.setVisibility(View.GONE);
        }
    }

    private void mostrarMensaje(String mensaje, boolean esError) {
        int color = esError ? R.color.ronda_error : R.color.ronda_primary;
        textoMensaje.setTextColor(ContextCompat.getColor(requireContext(), color));
        textoMensaje.setText(mensaje);
        textoMensaje.setVisibility(View.VISIBLE);
    }
}
