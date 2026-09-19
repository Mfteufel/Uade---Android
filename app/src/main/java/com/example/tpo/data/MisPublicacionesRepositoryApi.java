package com.example.tpo.data;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.tpo.data.remote.ApiClient;
import com.example.tpo.data.remote.ApiService;
import com.example.tpo.data.remote.dto.CambiarEstadoPublicacionRequest;
import com.example.tpo.data.remote.dto.MiPublicacionResponse;
import com.example.tpo.data.remote.dto.PublicacionCreadaResponse;
import com.example.tpo.model.BorradorPublicacion;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.MiPublicacion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Implementación de {@link MisPublicacionesRepository} con Retrofit.
 * <p>
 * Las fotos llegan como {@link Uri} de la galería (content://); antes de
 * mandarlas hay que leer sus bytes con el {@link ContentResolver}, lo que es
 * I/O y por eso se hace en {@link #executor} y no en el Main Thread. Una vez
 * armado el {@link MultipartBody.Part}, el {@code enqueue} de Retrofit ya es
 * asincrónico por sí mismo y en Android entrega la respuesta en el Main
 * Thread automáticamente, así que de ahí en más no hace falta ningún Handler
 * (a diferencia de {@code BorradorRepositoryLocal}, donde sí hace falta).
 */
public class MisPublicacionesRepositoryApi implements MisPublicacionesRepository {

    private final Context contextoApp;
    private final ApiService api = ApiClient.getInstancia();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MisPublicacionesRepositoryApi(Context context) {
        this.contextoApp = context.getApplicationContext();
    }

    @Override
    public void publicar(BorradorPublicacion borrador, RepositorioCallback<MiPublicacion> callback) {
        executor.execute(() -> {
            List<MultipartBody.Part> partesFotos;
            try {
                partesFotos = leerFotos(borrador.getFotos());
            } catch (IOException excepcion) {
                callback.onError("No pudimos leer alguna de las fotos elegidas");
                return;
            }

            RequestBody vendedorId = texto(SesionUsuario.getInstancia().getUsuarioId());
            RequestBody titulo = texto(borrador.getTitulo());
            RequestBody descripcion = texto(borrador.getDescripcion());
            RequestBody categoria = texto(borrador.getCategoria().name());
            RequestBody estadoArticulo = texto(borrador.getEstadoArticulo().name());
            RequestBody precio = texto(String.valueOf(borrador.getPrecio()));
            RequestBody zona = texto(borrador.getZona().name());

            api.crearPublicacion(vendedorId, titulo, descripcion, categoria, estadoArticulo,
                            precio, zona, partesFotos)
                    .enqueue(new Callback<PublicacionCreadaResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<PublicacionCreadaResponse> call,
                                               @NonNull Response<PublicacionCreadaResponse> response) {
                            if (!response.isSuccessful() || response.body() == null) {
                                callback.onError("No pudimos publicar el artículo");
                                return;
                            }
                            MiPublicacion creada = new MiPublicacion(
                                    response.body().getId(),
                                    borrador.getTitulo(),
                                    borrador.getPrecio(),
                                    null,
                                    borrador.getEstadoArticulo(),
                                    EstadoPublicacion.ACTIVA,
                                    System.currentTimeMillis());
                            callback.onExito(creada);
                        }

                        @Override
                        public void onFailure(@NonNull Call<PublicacionCreadaResponse> call,
                                              @NonNull Throwable throwable) {
                            callback.onError("No pudimos publicar el artículo. Revisá tu conexión.");
                        }
                    });
        });
    }

    @Override
    public void listar(RepositorioCallback<List<MiPublicacion>> callback) {
        api.listarMisPublicaciones(SesionUsuario.getInstancia().getUsuarioId())
                .enqueue(new Callback<List<MiPublicacionResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<MiPublicacionResponse>> call,
                                           @NonNull Response<List<MiPublicacionResponse>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("No pudimos cargar tus publicaciones");
                            return;
                        }
                        callback.onExito(haciaModelo(response.body()));
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<MiPublicacionResponse>> call,
                                          @NonNull Throwable throwable) {
                        callback.onError("No pudimos cargar tus publicaciones. Revisá tu conexión.");
                    }
                });
    }

    @Override
    public void pausar(String idPublicacion, RepositorioCallback<Void> callback) {
        cambiarEstado(idPublicacion, EstadoPublicacion.PAUSADA, callback);
    }

    @Override
    public void reactivar(String idPublicacion, RepositorioCallback<Void> callback) {
        cambiarEstado(idPublicacion, EstadoPublicacion.ACTIVA, callback);
    }

    private void cambiarEstado(String idPublicacion, EstadoPublicacion nuevoEstado,
                               RepositorioCallback<Void> callback) {
        CambiarEstadoPublicacionRequest request =
                new CambiarEstadoPublicacionRequest(nuevoEstado.name());
        api.cambiarEstadoPublicacion(idPublicacion, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError("No pudimos actualizar la publicación");
                    return;
                }
                callback.onExito(null);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
                callback.onError("No pudimos actualizar la publicación. Revisá tu conexión.");
            }
        });
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static List<MiPublicacion> haciaModelo(List<MiPublicacionResponse> respuestas) {
        List<MiPublicacion> resultado = new ArrayList<>();
        for (MiPublicacionResponse respuesta : respuestas) {
            resultado.add(new MiPublicacion(
                    respuesta.getId(),
                    respuesta.getTitulo(),
                    respuesta.getPrecio(),
                    respuesta.getFotoPrincipalUrl(),
                    EstadoArticulo.valueOf(respuesta.getEstadoArticulo()),
                    EstadoPublicacion.valueOf(respuesta.getEstadoPublicacion()),
                    respuesta.getFechaPublicacion()));
        }
        return resultado;
    }

    private static RequestBody texto(String valor) {
        return RequestBody.create(valor, MediaType.parse("text/plain"));
    }

    /** Lee cada foto de la galería y la envuelve como parte multipart. */
    private List<MultipartBody.Part> leerFotos(List<Uri> fotos) throws IOException {
        ContentResolver resolver = contextoApp.getContentResolver();
        List<MultipartBody.Part> partes = new ArrayList<>();
        for (int i = 0; i < fotos.size(); i++) {
            Uri uri = fotos.get(i);
            byte[] bytes = leerBytes(resolver, uri);
            String tipo = tipoMime(resolver, uri);
            RequestBody body = RequestBody.create(bytes, MediaType.parse(tipo));
            partes.add(MultipartBody.Part.createFormData("fotos", "foto_" + i, body));
        }
        return partes;
    }

    private static byte[] leerBytes(ContentResolver resolver, Uri uri) throws IOException {
        try (InputStream entrada = resolver.openInputStream(uri)) {
            if (entrada == null) {
                throw new IOException("No se pudo abrir " + uri);
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int leidos;
            while ((leidos = entrada.read(buffer)) != -1) {
                salida.write(buffer, 0, leidos);
            }
            return salida.toByteArray();
        }
    }

    private static String tipoMime(ContentResolver resolver, Uri uri) {
        String tipo = resolver.getType(uri);
        return tipo != null ? tipo : "image/jpeg";
    }
}
