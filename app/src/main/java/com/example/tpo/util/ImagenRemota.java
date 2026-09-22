package com.example.tpo.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Carga la foto de una publicación en un {@link ImageView} sin librería de
 * imágenes (Glide/Picasso no se vieron en clase, mismo criterio que
 * {@link ImagenUtils} para la foto de perfil).
 * <p>
 * Las fotos se sirven sin autenticación ({@code /fotos/...}, estático), así
 * que usa su propio {@link OkHttpClient} chico en vez del que arma
 * {@code NetworkModule} con el interceptor de token — no hace falta pasar por
 * Hilt para esto, es una utilidad de pintado, no un repositorio.
 */
public final class ImagenRemota {

    private static final OkHttpClient CLIENTE = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    /** Bitmaps ya decodificados, para no volver a bajar la misma foto en cada scroll del RecyclerView. */
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(30);

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private ImagenRemota() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Pinta {@code url} en {@code vista}, o el placeholder si no hay URL o falla
     * la descarga. Segura de llamar desde un {@code onBindViewHolder} que se
     * repite: {@code vista.setTag(url)} descarta la respuesta si para cuando
     * termina de bajar la vista ya se reusó para otra publicación.
     */
    @MainThread
    public static void cargarEn(ImageView vista, @Nullable String url, @DrawableRes int placeholder) {
        vista.setTag(url);

        if (url == null) {
            mostrarPlaceholder(vista, placeholder);
            return;
        }

        Bitmap cacheada = CACHE.get(url);
        if (cacheada != null) {
            mostrarFoto(vista, cacheada);
            return;
        }

        mostrarPlaceholder(vista, placeholder);
        EXECUTOR.execute(() -> {
            Bitmap bitmap = descargar(url);
            if (bitmap != null) {
                CACHE.put(url, bitmap);
            }
            PRINCIPAL.post(() -> {
                // La vista pudo reciclarse para otra publicación mientras bajaba esta foto.
                if (bitmap != null && url.equals(vista.getTag())) {
                    mostrarFoto(vista, bitmap);
                }
            });
        });
    }

    private static void mostrarPlaceholder(ImageView vista, @DrawableRes int placeholder) {
        vista.setScaleType(ImageView.ScaleType.CENTER);
        vista.setImageResource(placeholder);
    }

    private static void mostrarFoto(ImageView vista, Bitmap bitmap) {
        vista.setScaleType(ImageView.ScaleType.CENTER_CROP);
        vista.setImageBitmap(bitmap);
    }

    @Nullable
    private static Bitmap descargar(String url) {
        Request pedido = new Request.Builder().url(url).build();
        try (Response respuesta = CLIENTE.newCall(pedido).execute()) {
            if (!respuesta.isSuccessful()) {
                return null;
            }
            ResponseBody cuerpo = respuesta.body();
            if (cuerpo == null) {
                return null;
            }
            byte[] bytes = cuerpo.bytes();
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IOException | RuntimeException excepcion) {
            // Sin conexión, URL rota o respuesta que no es una imagen: se queda con el placeholder.
            return null;
        }
    }
}
