package com.example.tpo.login;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.tpo.R;

public final class Biometria {

    // huella o rostro, y como alternativa el PIN o patron del equipo
    private static final int AUTENTICADORES =
            Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL;

    private Biometria() {
    }

    // paso 1: preguntarle al sistema antes de armar nada
    public static boolean disponible(Context context) {
        int estado = BiometricManager.from(context).canAuthenticate(AUTENTICADORES);
        return estado == BiometricManager.BIOMETRIC_SUCCESS;
    }

    // pasos 2 y 3: armar el dialogo y lanzarlo
    public static void pedir(Fragment fragment, BiometricPrompt.AuthenticationCallback callback) {
        // con DEVICE_CREDENTIAL no se puede poner boton negativo: el escape ya es el PIN
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(fragment.getString(R.string.login_biometria_titulo))
                .setSubtitle(fragment.getString(R.string.login_biometria_subtitulo))
                .setAllowedAuthenticators(AUTENTICADORES)
                .build();

        BiometricPrompt prompt = new BiometricPrompt(
                fragment, ContextCompat.getMainExecutor(fragment.requireContext()), callback);
        prompt.authenticate(info);
    }

    // despues de entrar con credenciales se ofrece activar el desbloqueo, una sola vez
    public static void ofrecerActivar(Fragment fragment, TokenManager tokenManager, Runnable alTerminar) {
        if (tokenManager.isBiometricEnabled() || !disponible(fragment.requireContext())) {
            alTerminar.run();
            return;
        }
        new AlertDialog.Builder(fragment.requireContext())
                .setTitle(R.string.biometria_activar_titulo)
                .setMessage(R.string.biometria_activar_mensaje)
                .setCancelable(false)
                .setPositiveButton(R.string.biometria_activar_si, (dialogo, cual) -> {
                    tokenManager.setBiometricEnabled(true);
                    alTerminar.run();
                })
                .setNegativeButton(R.string.biometria_activar_no, (dialogo, cual) -> alTerminar.run())
                .show();
    }
}
