package com.example.tpo.login;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class TokenManager {

    private final SharedPreferences preferencias;

    @Inject
    public TokenManager(@ApplicationContext Context context) {
        preferencias = context.getSharedPreferences("sesion", Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        preferencias.edit().putString("token", token).apply();
    }

    public String getToken() {
        return preferencias.getString("token", null);
    }
}
