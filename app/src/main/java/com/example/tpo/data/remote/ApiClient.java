package com.example.tpo.data.remote;

import com.example.tpo.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Punto único de acceso a Retrofit.
 * <p>
 * La URL sale de {@code BuildConfig.API_BASE_URL}, que a su vez se arma desde
 * {@code local.properties} (ver {@code app/build.gradle.kts}): cada quien la
 * apunta a su propio backend de desarrollo (por ejemplo el FastAPI corriendo
 * en la red local) sin tocar este archivo. Sin esa propiedad, cae a un
 * placeholder que no responde.
 */
public final class ApiClient {

    private static final String BASE_URL = BuildConfig.API_BASE_URL;

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
