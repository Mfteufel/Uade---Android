package com.example.tpo.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.tpo.BuildConfig;
import com.example.tpo.MainActivity;
import com.example.tpo.model.Novedad;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * PollingService es un Foreground Service que realiza Long Polling contra el servidor para recibir novedades.
 * <p>
 * Long polling es una técnica en la que el cliente realiza una petición HTTP al servidor y éste retiene
 * la conexión (hasta 25 s) hasta que se produce un evento/novedad o vence el tiempo de espera.
 * <p>
 * Se utiliza un Foreground Service en lugar de un Service común porque a partir de Android 8 (API 26)
 * el sistema operativo restringe y elimina los servicios en segundo plano para ahorrar batería.
 * startForeground muestra una notificación fija e inamovible en la barra de estado y el sistema
 * mantiene vivo el servicio (aunque no sobrevive a un "Forzar detención" manual del usuario).
 */
@AndroidEntryPoint
public class PollingService extends Service {

    private static final String TAG = "PollingService";
    private static final String ENDPOINT_NOVEDADES = BuildConfig.API_BASE_URL + "novedades";
    private static final int NOTIF_ID_FOREGROUND = 1;
    private static final String CANAL_ID = "novedades_channel";
    private static final String CANAL_NOMBRE = "Novedades";

    @Inject
    OkHttpClient okHttpClient;

    // Cliente OkHttp derivado con un readTimeout mayor para long polling.
    private OkHttpClient clienteLongPolling;

    private final Gson gson = new Gson();
    private Thread pollingThread;
    // Llamada en curso: se cancela al destruir el servicio para no esperar los 25 s del servidor.
    private volatile Call llamadaActual;
    private String ultimoId = null;
    private int idNotificacion = 2;

    @Override
    public void onCreate() {
        super.onCreate();
        // El servidor retiene la conexión hasta 25 s, por lo que el cliente necesita su propio timeout, mayor (40 s).
        clienteLongPolling = okHttpClient.newBuilder()
                .readTimeout(40, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // El canal es obligatorio desde Android 8; si ya existe, esta llamada no hace nada.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID,
                    CANAL_NOMBRE,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(canal);
            }
        }

        // Notificación fija mientras corre el servicio. setOnlyAlertOnce evita que suene
        // cada vez que se vuelve al Home y se repite startForeground.
        Notification notifForeground = new NotificationCompat.Builder(this, CANAL_ID)
                .setContentTitle("Ronda")
                .setContentText("Escuchando preguntas y ofertas")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build();

        // Tiene que llamarse dentro de los 5 segundos, si no el sistema mata el servicio.
        startForeground(NOTIF_ID_FOREGROUND, notifForeground);

        // onStartCommand se llama cada vez que se entra al Home: si el hilo ya corre, no se crea otro.
        // La red nunca va en el hilo principal (NetworkOnMainThreadException).
        if (pollingThread == null || !pollingThread.isAlive()) {
            pollingThread = new Thread(this::loopDePolling);
            pollingThread.start();
        }

        // START_STICKY: si el sistema mata el servicio por memoria, lo vuelve a crear cuando puede.
        return START_STICKY;
    }

    private void loopDePolling() {
        while (!Thread.currentThread().isInterrupted()) {
            String url = ENDPOINT_NOVEDADES;
            if (ultimoId != null) {
                url += "?desde=" + ultimoId;
            }

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            llamadaActual = clienteLongPolling.newCall(request);
            try (Response res = llamadaActual.execute()) {
                if (res.code() == 200 && res.body() != null) {
                    Novedad[] novedades = gson.fromJson(res.body().string(), Novedad[].class);
                    if (novedades != null) {
                        for (Novedad n : novedades) {
                            mostrarNotificacion(n);
                            ultimoId = n.getId();
                        }
                    }
                } else if (res.code() == 204) {
                    // Sin novedades en 25 s: se vuelve a pedir sin pausa, la espera ya la hizo el servidor.
                } else if (res.code() == 401) {
                    Log.d(TAG, "Sesión inválida (401), deteniendo servicio");
                    stopSelf();
                    return;
                } else {
                    Log.w(TAG, "Código de respuesta no esperado: " + res.code());
                    pausar();
                }
            } catch (IOException e) {
                // Nunca matar el loop por un error de red, se reintenta en el próximo ciclo.
                Log.w(TAG, "Error de red en polling", e);
                pausar();
            }
        }
        Log.d(TAG, "Loop de polling finalizado");
    }

    private void pausar() {
        try {
            // Pausa antes de reintentar para no saturar al servidor si está caído.
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // sleep limpia la marca de interrupción: se vuelve a marcar para que el while termine.
            Thread.currentThread().interrupt();
        }
    }

    private void mostrarNotificacion(Novedad n) {
        // Desde Android 13 el permiso es en runtime: sin él no se puede mostrar el aviso.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Novedad.EXTRA_TIPO, n.getTipo());
        intent.putExtra(Novedad.EXTRA_PUBLICACION_ID, n.getPublicacionId());
        intent.putExtra(Novedad.EXTRA_OFERTA_ID, n.getOfertaId());
        intent.putExtra(Novedad.EXTRA_ROL, n.getRol());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                idNotificacion,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CANAL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(n.getTitulo())
                .setContentText(n.getCuerpo())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        manager.notify(idNotificacion++, builder.build());
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        // Desde Android 15 un servicio dataSync puede correr 6 h cada 24 h; al cumplirse, el sistema
        // llama a este método y hay que frenar el servicio en pocos segundos.
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (llamadaActual != null) {
            llamadaActual.cancel();
        }
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
