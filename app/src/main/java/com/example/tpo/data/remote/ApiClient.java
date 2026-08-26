package com.example.tpo.data.remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Punto único de acceso a Retrofit.
 * <p>
 * TODO: reemplazar por la URL real de la API_Rest del TPO en cuanto exista.
 * Mientras tanto apunta a un placeholder: la app compila y corre, pero
 * {@link ApiService} va a devolver error de red hasta que se cambie esto.
 */
public final class ApiClient {

    private static final String BASE_URL = "https://api.ronda.tpo.uade.edu.ar/";

    private static ApiService instancia;

    private ApiClient() {
        // Clase de utilidades: no se instancia.
    }

    public static synchronized ApiService getInstancia() {
        if (instancia == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient cliente = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            Gson gson = new GsonBuilder().create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(cliente)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            instancia = retrofit.create(ApiService.class);
        }
        return instancia;
    }
}
