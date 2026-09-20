package com.example.tpo.login;

import android.app.Application;
import android.content.Context;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class RondaApp extends Application {

    private static Application instancia;

    @Override
    public void onCreate() {
        super.onCreate();
        instancia = this;
    }

    /**
     * Escape hatch para singletons manuales viejos (p. ej. {@code PublicacionRepositoryMock})
     * que necesitan un {@code Context} para Room pero cuyo {@code getInstancia()} no lo recibe
     * como parámetro porque se llama desde inicializadores de campo de Fragments ajenos a este
     * punto, antes de que exista un Context de Fragment disponible.
     * <p>
     * No es el patrón a copiar para dependencias nuevas: esas van por Hilt (constructor
     * {@code @Inject} o {@code @Provides} en un módulo), no por un Context estático.
     */
    public static Context getContextoApp() {
        return instancia.getApplicationContext();
    }
}
