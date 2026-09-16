package com.example.tpo.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.data.FavoritoRepository;
import com.example.tpo.model.Publicacion;
import com.example.tpo.util.FormatoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

    /**
     * Aviso de que el usuario tocó el corazón de favorito de una tarjeta.
     * Igual que con el click de la tarjeta, el adapter no decide nada: ya
     * pintó el ícono de forma optimista
     * y le avisa al Fragment el estado nuevo para que llame al repositorio.
     */
    public interface OnFavoritoClickListener {
        void onFavoritoClick(Publicacion publicacion, boolean favoritoNuevo);
    }

    private final List<Publicacion> publicaciones = new ArrayList<>();
    private final FavoritoRepository favoritoRepositorio;
    private final OnPublicacionClickListener listener;
    private final OnFavoritoClickListener favoritoListener;
    /** IDs a destacar como "Nueva" porque matchean la búsqueda guardada recién aplicada (Punto 10). */
    private Set<String> idsNuevaBusqueda = Collections.emptySet();

    public PublicacionAdapter(FavoritoRepository favoritoRepositorio,
                              OnPublicacionClickListener listener,
                              OnFavoritoClickListener favoritoListener) {
        this.favoritoRepositorio = favoritoRepositorio;
        this.listener = listener;
        this.favoritoListener = favoritoListener;
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
        Publicacion publicacion = publicaciones.get(position);
        boolean esNuevaDeBusqueda = idsNuevaBusqueda.contains(publicacion.getId());
        holder.enlazar(publicacion, favoritoRepositorio, esNuevaDeBusqueda, listener, favoritoListener);
    }

    /** Punto 10: qué publicaciones destacar como "Nueva" al aplicar una búsqueda guardada con novedad. */
    public void marcarNuevasDeBusqueda(Set<String> ids) {
        idsNuevaBusqueda = ids;
        notifyDataSetChanged();
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
     * Vuelve a pintar el ícono de favorito de una publicación puntual leyendo
     * el estado real del repositorio. Se usa para corregir el pintado
     * optimista cuando marcar/desmarcar falla.
     */
    public void refrescarFavorito(String publicacionId) {
        for (int i = 0; i < publicaciones.size(); i++) {
            if (publicaciones.get(i).getId().equals(publicacionId)) {
                notifyItemChanged(i);
                return;
            }
        }
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
        private final View indicadorNuevaBusqueda;
        private final TextView precio;
        private final View indicadorNovedadPrecio;
        private final TextView estado;
        private final TextView zona;
        private final ImageButton botonFavorito;

        PublicacionViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.tituloPublicacion);
            indicadorNuevaBusqueda = itemView.findViewById(R.id.indicadorNuevaBusqueda);
            precio = itemView.findViewById(R.id.precioPublicacion);
            indicadorNovedadPrecio = itemView.findViewById(R.id.indicadorNovedadPrecio);
            estado = itemView.findViewById(R.id.estadoPublicacion);
            zona = itemView.findViewById(R.id.zonaPublicacion);
            botonFavorito = itemView.findViewById(R.id.botonFavorito);
        }

        void enlazar(Publicacion publicacion,
                     FavoritoRepository favoritoRepositorio,
                     boolean esNuevaDeBusqueda,
                     OnPublicacionClickListener listener,
                     OnFavoritoClickListener favoritoListener) {
            Context contexto = itemView.getContext();

            titulo.setText(publicacion.getTitulo());
            indicadorNuevaBusqueda.setVisibility(esNuevaDeBusqueda ? View.VISIBLE : View.GONE);
            precio.setText(FormatoUtils.precio(publicacion.getPrecio()));
            indicadorNovedadPrecio.setVisibility(
                    favoritoRepositorio.tieneNovedad(publicacion.getId()) ? View.VISIBLE : View.GONE);
            estado.setText(publicacion.getEstado().getEtiqueta());

            // Renglón "Caballito · hace 5 h"
            zona.setText(contexto.getString(
                    R.string.item_zona_y_fecha,
                    publicacion.getZona().getNombre(),
                    FormatoUtils.antiguedad(contexto, publicacion.getFechaPublicacion())));

            pintarFavorito(favoritoRepositorio.esFavorito(publicacion.getId()));
            botonFavorito.setOnClickListener(v -> {
                boolean favoritoNuevo = !favoritoRepositorio.esFavorito(publicacion.getId());
                // Se pinta sin esperar la respuesta del repositorio: si
                // falla, el Fragment llama a refrescarFavorito() para corregirlo.
                pintarFavorito(favoritoNuevo);
                favoritoListener.onFavoritoClick(publicacion, favoritoNuevo);
            });

            itemView.setOnClickListener(v -> listener.onPublicacionClick(publicacion));
        }

        private void pintarFavorito(boolean favorito) {
            Context contexto = itemView.getContext();
            botonFavorito.setImageResource(
                    favorito ? R.drawable.ic_favorito_lleno : R.drawable.ic_favorito_borde);
            botonFavorito.setContentDescription(contexto.getString(
                    favorito ? R.string.item_favorito_quitar : R.string.item_favorito_agregar));
        }
    }
}
