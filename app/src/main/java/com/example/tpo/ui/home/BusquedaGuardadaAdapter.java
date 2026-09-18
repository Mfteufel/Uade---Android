package com.example.tpo.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.data.BusquedaGuardadaRepository;
import com.example.tpo.model.BusquedaGuardada;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BusquedaGuardadaAdapter extends RecyclerView.Adapter<BusquedaGuardadaAdapter.BusquedaViewHolder> {

    private final List<BusquedaGuardada> busquedas = new ArrayList<>();
    private final BusquedaGuardadaRepository repositorio;
    private final Consumer<BusquedaGuardada> alElegir;
    private final Consumer<BusquedaGuardada> alEliminar;

    public BusquedaGuardadaAdapter(BusquedaGuardadaRepository repositorio,
                                   Consumer<BusquedaGuardada> alElegir,
                                   Consumer<BusquedaGuardada> alEliminar) {
        this.repositorio = repositorio;
        this.alElegir = alElegir;
        this.alEliminar = alEliminar;
    }

    public void reemplazar(List<BusquedaGuardada> nuevas) {
        busquedas.clear();
        busquedas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BusquedaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_busqueda_guardada, parent, false);
        return new BusquedaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull BusquedaViewHolder holder, int position) {
        holder.enlazar(busquedas.get(position), repositorio, alElegir, alEliminar);
    }

    @Override
    public int getItemCount() {
        return busquedas.size();
    }

    static class BusquedaViewHolder extends RecyclerView.ViewHolder {

        private final TextView nombre;
        private final View indicadorNovedad;
        private final ImageButton botonEliminar;

        BusquedaViewHolder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.nombreBusqueda);
            indicadorNovedad = itemView.findViewById(R.id.indicadorNovedadBusqueda);
            botonEliminar = itemView.findViewById(R.id.botonEliminarBusqueda);
        }

        void enlazar(BusquedaGuardada busqueda,
                     BusquedaGuardadaRepository repositorio,
                     Consumer<BusquedaGuardada> alElegir,
                     Consumer<BusquedaGuardada> alEliminar) {
            nombre.setText(busqueda.getNombre());
            indicadorNovedad.setVisibility(
                    repositorio.tieneNovedad(busqueda.getId()) ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> alElegir.accept(busqueda));
            botonEliminar.setOnClickListener(v -> alEliminar.accept(busqueda));
        }
    }
}
