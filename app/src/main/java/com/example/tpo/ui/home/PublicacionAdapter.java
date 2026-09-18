package com.example.tpo.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.Publicacion;
import com.example.tpo.util.FormatoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter del listado de publicaciones del Home.
 * <p>
 * Mantiene su propia copia de la lista y expone dos formas de actualizarla, que
 * es lo que necesita la paginación: {@link #reemplazar(List)} para una búsqueda
 * nueva y {@link #agregar(List)} para sumar la página siguiente sin perder el
 * scroll.
 */
public class PublicacionAdapter extends RecyclerView.Adapter<PublicacionAdapter.PublicacionViewHolder> {

    /**
     * Aviso de que el usuario tocó una tarjeta.
     * <p>
     * El adapter no navega ni sabe a dónde se va: solo avisa. La decisión de qué
     * hacer es del Fragment, que es el que tiene el NavController.
     */
    public interface OnPublicacionClickListener {
        void onPublicacionClick(Publicacion publicacion);
    }

    private final List<Publicacion> publicaciones = new ArrayList<>();
    private final OnPublicacionClickListener listener;

    public PublicacionAdapter(OnPublicacionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PublicacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // attachToRoot en false: de adjuntar la vista al padre se encarga el RecyclerView.
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_publicacion, parent, false);
        return new PublicacionViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull PublicacionViewHolder holder, int position) {
        holder.enlazar(publicaciones.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return publicaciones.size();
    }

    /** Reemplaza todo el contenido. Se usa cuando cambia la búsqueda o algún filtro. */
    public void reemplazar(List<Publicacion> nuevas) {
        publicaciones.clear();
        publicaciones.addAll(nuevas);
        // Cambió la lista entera, así que se redibuja completa. Para un listado
        // de este tamaño alcanza; con listas grandes correspondería DiffUtil.
        notifyDataSetChanged();
    }

    /** Suma la página siguiente al final, sin tocar lo que ya está en pantalla. */
    public void agregar(List<Publicacion> nuevas) {
        if (nuevas.isEmpty()) {
            return;
        }
        int desde = publicaciones.size();
        publicaciones.addAll(nuevas);
        // Solo se avisa del rango nuevo: así el RecyclerView no vuelve a dibujar
        // los ítems que ya estaban y el scroll no salta.
        notifyItemRangeInserted(desde, nuevas.size());
    }

    /** Vacía la lista (antes de una búsqueda nueva). */
    public void limpiar() {
        int cantidad = publicaciones.size();
        publicaciones.clear();
        notifyItemRangeRemoved(0, cantidad);
    }

    /**
     * ViewHolder de una tarjeta.
     * <p>
     * Los findViewById se hacen una sola vez acá, en el constructor. Ese es
     * justamente el punto del patrón: si se hicieran en onBindViewHolder se
     * repetirían en cada scroll.
     */
    static class PublicacionViewHolder extends RecyclerView.ViewHolder {

        private final TextView titulo;
        private final TextView precio;
        private final TextView estado;
        private final TextView estadoPublicacion;
        private final TextView zona;

        PublicacionViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.tituloPublicacion);
            precio = itemView.findViewById(R.id.precioPublicacion);
            estado = itemView.findViewById(R.id.estadoPublicacion);
            estadoPublicacion = itemView.findViewById(R.id.estadoPublicacionItem);
            zona = itemView.findViewById(R.id.zonaPublicacion);
        }

        void enlazar(Publicacion publicacion, OnPublicacionClickListener listener) {
            Context contexto = itemView.getContext();

            titulo.setText(publicacion.getTitulo());
            precio.setText(FormatoUtils.precio(publicacion.getPrecio()));
            estado.setText(publicacion.getEstado().getEtiqueta());

            // Badge de "Pausada"/"Vendida" — Punto 4, gestión de la publicación.
            // Solo lo ve el dueño: el mock ya oculta del listado las que no son
            // suyas y no están activas (ver PublicacionRepositoryMock.aplicarFiltros).
            if (publicacion.getEstadoPublicacion() == EstadoPublicacion.ACTIVA) {
                estadoPublicacion.setVisibility(View.GONE);
            } else {
                estadoPublicacion.setText(publicacion.getEstadoPublicacion().getEtiqueta());
                estadoPublicacion.setVisibility(View.VISIBLE);
            }

            // Renglón "Caballito · hace 5 h"
            zona.setText(contexto.getString(
                    R.string.item_zona_y_fecha,
                    publicacion.getZona().getNombre(),
                    FormatoUtils.antiguedad(contexto, publicacion.getFechaPublicacion())));

            itemView.setOnClickListener(v -> listener.onPublicacionClick(publicacion));
        }
    }
}
