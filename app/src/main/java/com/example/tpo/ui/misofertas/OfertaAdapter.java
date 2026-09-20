package com.example.tpo.ui.misofertas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.util.FormatoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de "Mis ofertas" (Punto 7). Igual que {@code MiPublicacionAdapter}, no usa
 * {@code DiffUtil}: la lista es chica y se reemplaza entera en cada carga de tab.
 * <p>
 * Muestra la misma fila para las dos tabs (Enviadas/Recibidas); lo único que cambia
 * es de quién es "la contraparte": en Enviadas es el vendedor de la publicación, en
 * Recibidas es el autor de la oferta. Ese dato no vive en {@link com.example.tpo.model.Oferta}
 * (no guarda el nombre del vendedor), así que {@code soyComprador} decide de dónde sacarlo.
 */
public class OfertaAdapter extends RecyclerView.Adapter<OfertaAdapter.OfertaViewHolder> {

    public interface OnOfertaClickListener {
        void onOfertaClick(FilaOferta fila);
    }

    private final List<FilaOferta> filas = new ArrayList<>();
    private final OnOfertaClickListener listener;
    private boolean soyComprador;

    public OfertaAdapter(OnOfertaClickListener listener) {
        this.listener = listener;
    }

    public void reemplazar(List<FilaOferta> nuevas, boolean soyComprador) {
        this.soyComprador = soyComprador;
        filas.clear();
        filas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OfertaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OfertaViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_oferta, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull OfertaViewHolder holder, int position) {
        holder.vincular(filas.get(position), soyComprador);
    }

    @Override
    public int getItemCount() {
        return filas.size();
    }

    class OfertaViewHolder extends RecyclerView.ViewHolder {

        private final TextView tituloPublicacionOferta;
        private final TextView contraparteOferta;
        private final TextView montoOferta;
        private final TextView estadoOferta;

        OfertaViewHolder(@NonNull View itemView) {
            super(itemView);
            tituloPublicacionOferta = itemView.findViewById(R.id.tituloPublicacionOferta);
            contraparteOferta = itemView.findViewById(R.id.contraparteOferta);
            montoOferta = itemView.findViewById(R.id.montoOferta);
            estadoOferta = itemView.findViewById(R.id.estadoOferta);
        }

        void vincular(FilaOferta fila, boolean soyComprador) {
            tituloPublicacionOferta.setText(fila.getPublicacion().getTitulo());
            montoOferta.setText(FormatoUtils.precio(fila.getOferta().getMonto()));
            estadoOferta.setText(estadoOferta.getContext()
                    .getString(fila.getOferta().getEstado().getEtiqueta()));

            String nombreContraparte = soyComprador
                    ? fila.getPublicacion().getVendedor().getNombre()
                    : fila.getOferta().getAutorNombre();
            contraparteOferta.setText(contraparteOferta.getContext().getString(
                    soyComprador ? R.string.item_oferta_a : R.string.item_oferta_de,
                    nombreContraparte));

            itemView.setOnClickListener(v -> listener.onOfertaClick(fila));
        }
    }
}
