package com.example.tpo.ui.mispublicaciones;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.MiPublicacion;
import com.example.tpo.util.FormatoUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de "Mis publicaciones" (Punto 5).
 * <p>
 * Igual que {@link com.example.tpo.ui.home.PublicacionAdapter} del Home, no usa
 * {@code DiffUtil}: la lista es chica (las publicaciones de un solo usuario) y
 * se reemplaza entera en cada carga, así que no vale la pena el cálculo de diff.
 */
public class MiPublicacionAdapter extends RecyclerView.Adapter<MiPublicacionAdapter.MiPublicacionViewHolder> {

    public interface OnAccionClickListener {
        void onPausar(MiPublicacion publicacion);
        void onReactivar(MiPublicacion publicacion);
    }

    private final List<MiPublicacion> publicaciones = new ArrayList<>();
    private final OnAccionClickListener listener;

    public MiPublicacionAdapter(OnAccionClickListener listener) {
        this.listener = listener;
    }

    public void reemplazar(List<MiPublicacion> nuevas) {
        publicaciones.clear();
        publicaciones.addAll(nuevas);
        notifyDataSetChanged();
    }

    /** Actualiza en el lugar el estado de una publicación, sin recargar toda la lista. */
    public void actualizarEstado(String id, EstadoPublicacion nuevoEstado) {
        for (int i = 0; i < publicaciones.size(); i++) {
            MiPublicacion actual = publicaciones.get(i);
            if (actual.getId().equals(id)) {
                publicaciones.set(i, new MiPublicacion(
                        actual.getId(), actual.getTitulo(), actual.getPrecio(),
                        actual.getFotoPrincipalUrl(), actual.getEstadoArticulo(),
                        nuevoEstado, actual.getFechaPublicacion()));
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public MiPublicacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MiPublicacionViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mi_publicacion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MiPublicacionViewHolder holder, int position) {
        holder.vincular(publicaciones.get(position));
    }

    @Override
    public int getItemCount() {
        return publicaciones.size();
    }

    class MiPublicacionViewHolder extends RecyclerView.ViewHolder {

        private final ImageView fotoPublicacion;
        private final TextView tituloPublicacion;
        private final TextView precioPublicacion;
        private final TextView estadoPublicacion;
        private final MaterialButton botonAccion;

        MiPublicacionViewHolder(@NonNull View itemView) {
            super(itemView);
            fotoPublicacion = itemView.findViewById(R.id.fotoPublicacion);
            tituloPublicacion = itemView.findViewById(R.id.tituloPublicacion);
            precioPublicacion = itemView.findViewById(R.id.precioPublicacion);
            estadoPublicacion = itemView.findViewById(R.id.estadoPublicacion);
            botonAccion = itemView.findViewById(R.id.botonAccion);
        }

        void vincular(MiPublicacion publicacion) {
            tituloPublicacion.setText(publicacion.getTitulo());
            precioPublicacion.setText(FormatoUtils.precio(publicacion.getPrecio()));
            estadoPublicacion.setText(estadoPublicacion.getContext()
                    .getString(publicacion.getEstadoPublicacion().getEtiqueta()));

            // La foto llega por URL desde la API; sin librería de carga de
            // imágenes (ver ic_imagen.xml) se muestra el placeholder siempre,
            // igual que hoy hace el Home con las publicaciones ajenas.
            fotoPublicacion.setImageResource(R.drawable.ic_imagen);

            switch (publicacion.getEstadoPublicacion()) {
                case ACTIVA:
                    botonAccion.setVisibility(View.VISIBLE);
                    botonAccion.setText(R.string.accion_pausar);
                    botonAccion.setOnClickListener(v -> listener.onPausar(publicacion));
                    break;
                case PAUSADA:
                    botonAccion.setVisibility(View.VISIBLE);
                    botonAccion.setText(R.string.accion_reactivar);
                    botonAccion.setOnClickListener(v -> listener.onReactivar(publicacion));
                    break;
                case VENDIDA:
                default:
                    // Sin acciones disponibles una vez vendida.
                    botonAccion.setVisibility(View.GONE);
                    botonAccion.setOnClickListener(null);
                    break;
            }
        }
    }
}
