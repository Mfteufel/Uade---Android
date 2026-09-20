package com.example.tpo.ui.publicar;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de las miniaturas del paso 1 del wizard (fotos ya elegidas).
 * <p>
 * Es deliberadamente simple: no hay paginación ni carga asincrónica porque las
 * fotos ya están en el dispositivo (vienen del selector de la galería), solo
 * hay que mostrarlas y permitir sacarlas.
 */
public class FotoSeleccionadaAdapter extends RecyclerView.Adapter<FotoSeleccionadaAdapter.FotoViewHolder> {

    public interface OnQuitarFotoListener {
        void onQuitarFoto(int posicion);
    }

    private final List<Uri> fotos = new ArrayList<>();
    private final OnQuitarFotoListener listener;

    public FotoSeleccionadaAdapter(OnQuitarFotoListener listener) {
        this.listener = listener;
    }

    public void actualizar(List<Uri> nuevasFotos) {
        fotos.clear();
        fotos.addAll(nuevasFotos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FotoViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_foto_seleccionada, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FotoViewHolder holder, int position) {
        holder.vincular(fotos.get(position));
    }

    @Override
    public int getItemCount() {
        return fotos.size();
    }

    class FotoViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imagenFoto;

        FotoViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
            imagenFoto = itemView.findViewById(R.id.imagenFoto);
            itemView.findViewById(R.id.botonQuitarFoto).setOnClickListener(v -> {
                int posicion = getBindingAdapterPosition();
                if (posicion != RecyclerView.NO_POSITION) {
                    listener.onQuitarFoto(posicion);
                }
            });
        }

        void vincular(Uri foto) {
            // Una foto elegida en una sesión anterior puede venir de un borrador
            // guardado en Room cuyo permiso de lectura del Photo Picker ya no es
            // válido (se revoca al reiniciarse el proceso si no se hizo
            // takePersistableUriPermission a tiempo). Sin este catch, esa
            // SecurityException tira abajo toda la pantalla.
            try {
                imagenFoto.setImageBitmap(decodificarMiniatura(itemView.getContext(), foto));
            } catch (SecurityException excepcion) {
                Log.w("FotoSeleccionadaAdapter", "Sin permiso para leer " + foto, excepcion);
                imagenFoto.setImageResource(R.drawable.ic_imagen);
            } catch (IOException excepcion) {
                Log.w("FotoSeleccionadaAdapter", "No se pudo leer " + foto, excepcion);
                imagenFoto.setImageResource(R.drawable.ic_imagen);
            }
        }

        // Las fotos de la galería vienen a resolución de cámara (o más, si son
        // screenshots): decodificarlas enteras para una miniatura de 96dp hacía
        // que Canvas rechazara dibujar el bitmap ("trying to draw too large
        // bitmap") y tirara abajo toda la Activity. Se decodifica en dos pasadas
        // (bordes primero, después el bitmap ya submuestreado al tamaño del ítem).
        private Bitmap decodificarMiniatura(Context context, Uri foto) throws IOException {
            int tamanioObjetivoPx = context.getResources().getDimensionPixelSize(R.dimen.item_foto_ancho);
            ContentResolver resolver = context.getContentResolver();

            BitmapFactory.Options limites = new BitmapFactory.Options();
            limites.inJustDecodeBounds = true;
            try (InputStream entrada = resolver.openInputStream(foto)) {
                if (entrada == null) {
                    throw new IOException("No se pudo abrir " + foto);
                }
                BitmapFactory.decodeStream(entrada, null, limites);
            }

            int muestreo = 1;
            while (limites.outWidth / (muestreo * 2) >= tamanioObjetivoPx
                    && limites.outHeight / (muestreo * 2) >= tamanioObjetivoPx) {
                muestreo *= 2;
            }

            BitmapFactory.Options opciones = new BitmapFactory.Options();
            opciones.inSampleSize = muestreo;
            try (InputStream entrada = resolver.openInputStream(foto)) {
                if (entrada == null) {
                    throw new IOException("No se pudo abrir " + foto);
                }
                Bitmap bitmap = BitmapFactory.decodeStream(entrada, null, opciones);
                if (bitmap == null) {
                    throw new IOException("No se pudo decodificar " + foto);
                }
                return bitmap;
            }
        }
    }
}
