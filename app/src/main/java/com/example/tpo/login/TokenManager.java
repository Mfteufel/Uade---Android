package com.example.tpo.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class TokenManager {

    private static final String TAG = "TokenManager";

    private final SharedPreferences preferencias;
    private final SharedPreferences ajustes;

    @Inject
    public TokenManager(@ApplicationContext Context context) {
        preferencias = abrirPreferencias(context);
        ajustes = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE);
    }

    // el token es un dato sensible: se guarda cifrado
    private SharedPreferences abrirPreferencias(Context context) {
        try {
            return EncryptedSharedPreferences.create(
                    "sesion_segura",
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "No se pudo abrir el almacenamiento cifrado: " + e.getMessage());
            return context.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        }
    }

    public void saveToken(String token) {
        preferencias.edit().putString("token", token).apply();
    }

    public String getToken() {
        return preferencias.getString("token", null);
    }

    public boolean isBiometricEnabled() {
        return ajustes.getBoolean("biometria", false);
    }

    public void setBiometricEnabled(boolean activada) {
        ajustes.edit().putBoolean("biometria", activada).apply();
    }

    public void clearToken() {
        preferencias.edit().remove("token").apply();
    }
}
