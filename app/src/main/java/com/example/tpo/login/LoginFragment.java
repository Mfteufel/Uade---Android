package com.example.tpo.login;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.tpo.R;
import com.example.tpo.data.AuthRepository;
import com.example.tpo.data.RepositorioCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Login — Punto 1 del TPO.
 * <p>
 * Cuatro estados sobre un solo layout (ver {@code fragment_login.xml}): desbloqueo
 * biométrico (si ya hay un token guardado), usuario/contraseña, pedido de OTP por
 * email, y verificación del código. {@link AuthRepository} habla con el backend
 * real desde el día uno — no hay mock para login.
 */
@AndroidEntryPoint
public class LoginFragment extends Fragment {

    private static final int AUTENTICADORES_BIOMETRIA =
            BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    private static final int SEGUNDOS_ENTRE_REENVIOS = 30;

    private enum Estado { BIOMETRIA, PASSWORD, OTP_EMAIL, OTP_CODIGO }

    @Inject
    AuthRepository authRepository;

    @Inject
    TokenManager tokenManager;

    private CircularProgressIndicator progreso;

    private View grupoBiometria;
    private MaterialButton botonUsarBiometria;

    private View grupoPassword;
    private TextInputEditText campoUsuario;
    private TextInputEditText campoClave;
    private MaterialButton botonIngresar;

    private View grupoOtpEmail;
    private TextInputEditText campoOtpEmail;
    private MaterialButton botonEnviarCodigo;

    private View grupoOtpCodigo;
    private TextInputEditText campoCodigo;
    private MaterialButton botonVerificarCodigo;
    private MaterialButton botonReenviarCodigo;

    /** Email para el que se pidió el código, necesario en el paso de verificar/reenviar. */
    @Nullable
    private String emailOtpActual;

    @Nullable
    private CountDownTimer temporizadorReenvio;

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

        progreso = view.findViewById(R.id.progresoLogin);

        grupoBiometria = view.findViewById(R.id.grupoBiometria);
        botonUsarBiometria = view.findViewById(R.id.botonUsarBiometria);

        grupoPassword = view.findViewById(R.id.grupoPassword);
        campoUsuario = view.findViewById(R.id.campoUsuario);
        campoClave = view.findViewById(R.id.campoClave);
        botonIngresar = view.findViewById(R.id.botonIngresar);

        grupoOtpEmail = view.findViewById(R.id.grupoOtpEmail);
        campoOtpEmail = view.findViewById(R.id.campoOtpEmail);
        botonEnviarCodigo = view.findViewById(R.id.botonEnviarCodigo);

        grupoOtpCodigo = view.findViewById(R.id.grupoOtpCodigo);
        campoCodigo = view.findViewById(R.id.campoCodigo);
        botonVerificarCodigo = view.findViewById(R.id.botonVerificarCodigo);
        botonReenviarCodigo = view.findViewById(R.id.botonReenviarCodigo);

        botonUsarBiometria.setOnClickListener(v -> lanzarBiometria());
        view.findViewById(R.id.botonUsarClaveEnVezDeBiometria)
                .setOnClickListener(v -> mostrarEstado(Estado.PASSWORD));

        botonIngresar.setOnClickListener(v -> iniciarSesionConPassword());
        view.findViewById(R.id.botonIrAOtp).setOnClickListener(v -> mostrarEstado(Estado.OTP_EMAIL));

        botonEnviarCodigo.setOnClickListener(v -> solicitarCodigo());
        view.findViewById(R.id.botonVolverAClave).setOnClickListener(v -> mostrarEstado(Estado.PASSWORD));

        botonVerificarCodigo.setOnClickListener(v -> verificarCodigo());
        botonReenviarCodigo.setOnClickListener(v -> reenviarCodigo());
        view.findViewById(R.id.botonCancelarOtp).setOnClickListener(v -> mostrarEstado(Estado.OTP_EMAIL));

        decidirEstadoInicial();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (temporizadorReenvio != null) {
            temporizadorReenvio.cancel();
            temporizadorReenvio = null;
        }
    }

    // ---------------------------------------------------------------------
    // Estados
    // ---------------------------------------------------------------------

    private void decidirEstadoInicial() {
        if (tokenManager.getToken() != null && biometriaDisponible()) {
            mostrarEstado(Estado.BIOMETRIA);
            lanzarBiometria();
        } else {
            mostrarEstado(Estado.PASSWORD);
        }
    }

    private void mostrarEstado(Estado estado) {
        grupoBiometria.setVisibility(estado == Estado.BIOMETRIA ? View.VISIBLE : View.GONE);
        grupoPassword.setVisibility(estado == Estado.PASSWORD ? View.VISIBLE : View.GONE);
        grupoOtpEmail.setVisibility(estado == Estado.OTP_EMAIL ? View.VISIBLE : View.GONE);
        grupoOtpCodigo.setVisibility(estado == Estado.OTP_CODIGO ? View.VISIBLE : View.GONE);
    }

    private void mostrarCargando(boolean cargando) {
        if (progreso == null) {
            return;
        }
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonIngresar.setEnabled(!cargando);
        botonEnviarCodigo.setEnabled(!cargando);
        botonVerificarCodigo.setEnabled(!cargando);
    }

    private void mostrarError(String mensaje) {
        if (getView() == null) {
            return;
        }
        Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show();
    }

    // ---------------------------------------------------------------------
    // Biometría
    // ---------------------------------------------------------------------

    private boolean biometriaDisponible() {
        return BiometricManager.from(requireContext()).canAuthenticate(AUTENTICADORES_BIOMETRIA)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /**
     * No se le puede poner botón "Cancelar" propio al prompt: la API tira
     * excepción si se combina {@code setNegativeButtonText} con
     * {@code DEVICE_CREDENTIAL} en los autenticadores permitidos. El escape
     * manual es el link "Usar contraseña" del layout.
     */
    private void lanzarBiometria() {
        Executor executorPrincipal = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt prompt = new BiometricPrompt(this, executorPrincipal,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult resultado) {
                        restaurarSesion();
                    }

                    @Override
                    public void onAuthenticationError(int codigoError, @NonNull CharSequence mensaje) {
                        // El usuario canceló o hubo un error del sistema (por ejemplo,
                        // demasiados intentos fallidos): se queda en la pantalla de
                        // biometría, con "Usar contraseña" disponible como salida.
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.login_biometria_titulo))
                .setSubtitle(getString(R.string.login_biometria_subtitulo))
                .setAllowedAuthenticators(AUTENTICADORES_BIOMETRIA)
                .build();
        prompt.authenticate(info);
    }

    /**
     * {@code SesionUsuario} es un singleton en memoria: si el proceso murió (caso
     * típico de volver a abrir la app), sobrevive el token pero no quién era el
     * usuario. Por eso el desbloqueo biométrico pide {@code GET /auth/sesion} en
     * vez de navegar directo a Home.
     */
    private void restaurarSesion() {
        mostrarCargando(true);
        authRepository.restaurarSesion(new RepositorioCallback<Void>() {
            @Override
            public void onExito(Void resultado) {
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                irAHome();
            }

            @Override
            public void onError(String mensaje) {
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                // El token guardado ya no sirve (venció o se revocó): no tiene sentido
                // insistir con biometría para un token muerto.
                tokenManager.clearToken();
                mostrarEstado(Estado.PASSWORD);
                mostrarError(mensaje);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Usuario y contraseña
    // ---------------------------------------------------------------------

    private void iniciarSesionConPassword() {
        String email = textoDe(campoUsuario);
        String clave = textoDe(campoClave);
        if (email.isEmpty() || clave.isEmpty()) {
            mostrarError(getString(R.string.login_error_campos_vacios));
            return;
        }

        mostrarCargando(true);
        authRepository.iniciarSesionConPassword(email, clave, new RepositorioCallback<Void>() {
            @Override
            public void onExito(Void resultado) {
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                irAHome();
            }

            @Override
            public void onError(String mensaje) {
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                mostrarError(mensaje);
            }
        });
    }

    // ---------------------------------------------------------------------
    // OTP
    // ---------------------------------------------------------------------

    private void solicitarCodigo() {
        String email = textoDe(campoOtpEmail);
        if (email.isEmpty()) {
            mostrarError(getString(R.string.login_error_email_vacio));
            return;
        }

        mostrarCargando(true);
        authRepository.solicitarCodigo(email, new RepositorioCallback<String>() {
            @Override
            public void onExito(String mensaje) {
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                emailOtpActual = email;
                campoCodigo.setText(null);
                mostrarEstado(Estado.OTP_CODIGO);
                iniciarCuentaRegresivaReenvio();
            }

            @Override
            public void onError(String mensaje) {
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                mostrarError(mensaje);
            }
        });
    }

    private void reenviarCodigo() {
        if (emailOtpActual == null) {
            return;
        }
        authRepository.reenviarCodigo(emailOtpActual, new RepositorioCallback<String>() {
            @Override
            public void onExito(String mensaje) {
                if (getView() == null) {
                    return;
                }
                mostrarError(mensaje);
                iniciarCuentaRegresivaReenvio();
            }

            @Override
            public void onError(String mensaje) {
                if (getView() == null) {
                    return;
                }
                mostrarError(mensaje);
            }
        });
    }

    private void verificarCodigo() {
        String codigo = textoDe(campoCodigo);
        if (emailOtpActual == null || codigo.isEmpty()) {
            mostrarError(getString(R.string.login_error_codigo_vacio));
            return;
        }

        mostrarCargando(true);
        authRepository.verificarCodigo(emailOtpActual, codigo, new RepositorioCallback<Void>() {
            @Override
            public void onExito(Void resultado) {
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                irAHome();
            }

            @Override
            public void onError(String mensaje) {
                if (getView() == null) {
                    return;
                }
                mostrarCargando(false);
                mostrarError(mensaje);
            }
        });
    }

    /** 30s antes de poder reenviar, mismo límite que ya aplica el backend (429 si se pide antes). */
    private void iniciarCuentaRegresivaReenvio() {
        if (temporizadorReenvio != null) {
            temporizadorReenvio.cancel();
        }
        botonReenviarCodigo.setEnabled(false);
        temporizadorReenvio = new CountDownTimer(SEGUNDOS_ENTRE_REENVIOS * 1000L, 1000L) {
            @Override
            public void onTick(long msRestantes) {
                botonReenviarCodigo.setText(getString(
                        R.string.login_reenviar_en_segundos, (msRestantes / 1000) + 1));
            }

            @Override
            public void onFinish() {
                botonReenviarCodigo.setEnabled(true);
                botonReenviarCodigo.setText(R.string.login_reenviar_codigo);
            }
        }.start();
    }

    // ---------------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------------

    private void irAHome() {
        Navigation.findNavController(requireView()).navigate(R.id.action_login_to_home);
    }

    /** Texto del campo, sin espacios sobrantes y nunca null. */
    private String textoDe(TextInputEditText campo) {
        CharSequence texto = campo.getText();
        return TextUtils.isEmpty(texto) ? "" : texto.toString().trim();
    }
}
