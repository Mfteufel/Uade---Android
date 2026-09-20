package com.example.tpo.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Preparación de la foto de perfil sin librerías de imágenes (Glide/Picasso no
 * se vieron en clase, igual que en el resto de la app).
 * <p>
 * Todos los métodos leen disco o decodifican bitmaps: son lentos y NUNCA se llaman
 * desde el Main Thread (de ahí el {@link WorkerThread}). Los repositorios los
 * corren en su propio executor.
 */
public final class ImagenUtils {

    /** Lado máximo de la foto de perfil que se sube: alcanza para un avatar y pesa pocos KB. */
    public static final int LADO_MAXIMO_FOTO_PERFIL = 512;

    private static final int CALIDAD_JPEG = 85;

    private ImagenUtils() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Lee la imagen elegida, la achica para que su lado mayor no pase de
     * {@code ladoMaximo}, la endereza según su orientación EXIF (las fotos de
     * cámara suelen venir "acostadas") y la comprime a JPEG.
     *
     * @throws IOException si la imagen no se puede leer o no es una imagen.
     */
    @WorkerThread
    public static byte[] comprimirJpeg(ContentResolver resolver, Uri uri, int ladoMaximo)
            throws IOException {
        // Primera pasada: solo las dimensiones, sin cargar los píxeles en memoria.
        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        try (InputStream entrada = abrir(resolver, uri)) {
            BitmapFactory.decodeStream(entrada, null, limites);
        }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) {
            throw new IOException("El archivo elegido no es una imagen");
        }

        // Segunda pasada: decodifica ya reducida por una potencia de 2, que es
        // mucho más barato que cargar una foto de 12 MP y achicarla después.
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = calcularReduccion(limites.outWidth, limites.outHeight, ladoMaximo);
        Bitmap imagen;
        try (InputStream entrada = abrir(resolver, uri)) {
            imagen = BitmapFactory.decodeStream(entrada, null, opciones);
        }
        if (imagen == null) {
            throw new IOException("No pudimos leer la imagen");
        }

        imagen = escalar(imagen, ladoMaximo);
        imagen = enderezar(imagen, leerRotacion(resolver, uri));

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        imagen.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida);
        return salida.toByteArray();
    }

    /** Decodifica los bytes de una imagen, o {@code null} si no son una imagen válida. */
    @WorkerThread
    @Nullable
    public static Bitmap decodificar(byte[] bytes) {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private static InputStream abrir(ContentResolver resolver, Uri uri) throws IOException {
        InputStream entrada = resolver.openInputStream(uri);
        if (entrada == null) {
            throw new IOException("No pudimos abrir la imagen");
        }
        return entrada;
    }

    private static int calcularReduccion(int ancho, int alto, int ladoMaximo) {
        int reduccion = 1;
        while (Math.max(ancho, alto) / (reduccion * 2) >= ladoMaximo) {
            reduccion *= 2;
        }
        return reduccion;
    }

    private static Bitmap escalar(Bitmap imagen, int ladoMaximo) {
        int mayor = Math.max(imagen.getWidth(), imagen.getHeight());
        if (mayor <= ladoMaximo) {
            return imagen;
        }
        float factor = (float) ladoMaximo / mayor;
        return Bitmap.createScaledBitmap(imagen,
                Math.round(imagen.getWidth() * factor),
                Math.round(imagen.getHeight() * factor), true);
    }

    /** Grados que hay que rotar según el EXIF. Si no se puede leer, se asume 0. */
    private static int leerRotacion(ContentResolver resolver, Uri uri) {
        // android.media.ExifInterface acepta un InputStream desde API 24, que es
        // justo el minSdk: no hace falta sumar la librería de AndroidX.
        try (InputStream entrada = abrir(resolver, uri)) {
            int orientacion = new ExifInterface(entrada).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientacion) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException excepcion) {
            return 0;
        }
    }

    private static Bitmap enderezar(Bitmap imagen, int grados) {
        if (grados == 0) {
            return imagen;
        }
        Matrix matriz = new Matrix();
        matriz.postRotate(grados);
        return Bitmap.createBitmap(imagen, 0, 0, imagen.getWidth(), imagen.getHeight(), matriz, true);
    }
}
