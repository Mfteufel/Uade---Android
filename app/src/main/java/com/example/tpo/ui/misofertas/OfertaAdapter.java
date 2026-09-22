package com.example.tpo.ui.misofertas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.data.SesionUsuario;
import com.example.tpo.model.OfertaNegociacion;
import com.example.tpo.util.FormatoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de "Mis ofertas" (Punto 7). Igual que {@code MiPublicacionAdapter}, no usa
 * {@code DiffUtil}: la lista es chica y se reemplaza entera en cada carga de tab.
 * <p>
 * Muestra la misma fila para las dos tabs (Enviadas/Recibidas); lo único que cambia
 * es el texto ("A" vs "De") delante del nombre de la contraparte, resuelto con
 * {@link OfertaNegociacion#nombreContraparte}.
 */
public class OfertaAdapter extends RecyclerView.Adapter<OfertaAdapter.OfertaViewHolder> {

    public interface OnOfertaClickListener {
        void onOfertaClick(OfertaNegociacion oferta);
    }

    private final List<OfertaNegociacion> ofertas = new ArrayList<>();
    private final OnOfertaClickListener listener;
    private boolean soyComprador;

    public OfertaAdapter(OnOfertaClickListener listener) {
        this.listener = listener;
    }

    public void reemplazar(List<OfertaNegociacion> nuevas, boolean soyComprador) {
        this.soyComprador = soyComprador;
        ofertas.clear();
        ofertas.addAll(nuevas);
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
        holder.vincular(ofertas.get(position), soyComprador);
    }

    @Override
    public int getItemCount() {
        return ofertas.size();
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

        void vincular(OfertaNegociacion oferta, boolean soyComprador) {
            tituloPublicacionOferta.setText(oferta.getTituloPublicacion());
            montoOferta.setText(FormatoUtils.precio(oferta.getPrecio()));
            estadoOferta.setText(estadoOferta.getContext()
                    .getString(oferta.getEstado().getEtiqueta()));

            String idUsuario = SesionUsuario.getInstancia().getUsuarioId();
            contraparteOferta.setText(contraparteOferta.getContext().getString(
                    soyComprador ? R.string.item_oferta_a : R.string.item_oferta_de,
                    oferta.nombreContraparte(idUsuario)));

            itemView.setOnClickListener(v -> listener.onOfertaClick(oferta));
        }
    }
}
