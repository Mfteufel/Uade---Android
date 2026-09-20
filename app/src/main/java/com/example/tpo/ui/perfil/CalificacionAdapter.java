package com.example.tpo.ui.perfil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.model.Calificacion;
import com.example.tpo.model.UsuarioResumen;
import com.example.tpo.util.FormatoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de las calificaciones recibidas que se muestran en el perfil público.
 * <p>
 * Mismo criterio que {@code PublicacionAdapter}: guarda su propia copia de la
 * lista y no navega; avisa al Fragment cuando se toca el autor.
 */
public class CalificacionAdapter extends RecyclerView.Adapter<CalificacionAdapter.CalificacionViewHolder> {

    /** Aviso de que se tocó una calificación: el Fragment abre el perfil de su autor. */
    public interface OnAutorClickListener {
        void onAutorClick(UsuarioResumen autor);
    }

    private final List<Calificacion> calificaciones = new ArrayList<>();
    private final OnAutorClickListener listener;

    public CalificacionAdapter(OnAutorClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalificacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calificacion, parent, false);
        return new CalificacionViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull CalificacionViewHolder holder, int position) {
        holder.enlazar(calificaciones.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return calificaciones.size();
    }

    public void reemplazar(List<Calificacion> nuevas) {
        calificaciones.clear();
        calificaciones.addAll(nuevas);
        notifyDataSetChanged();
    }

    static class CalificacionViewHolder extends RecyclerView.ViewHolder {

        private final TextView autor;
        private final TextView fecha;
        private final RatingBar estrellas;
        private final TextView articulo;
        private final TextView comentario;

        CalificacionViewHolder(@NonNull View vista) {
            super(vista);
            autor = vista.findViewById(R.id.textoAutorCalificacion);
            fecha = vista.findViewById(R.id.textoFechaCalificacion);
            estrellas = vista.findViewById(R.id.barraEstrellasCalificacion);
            articulo = vista.findViewById(R.id.textoArticuloCalificacion);
            comentario = vista.findViewById(R.id.textoComentarioCalificacion);
        }

        void enlazar(Calificacion calificacion, OnAutorClickListener listener) {
            autor.setText(calificacion.getAutor().getNombre());
            fecha.setText(FormatoUtils.fechaCompleta(calificacion.getFecha()));
            estrellas.setRating(calificacion.getEstrellas());
            // La RatingBar no se lee bien con TalkBack: se describe con palabras.
            estrellas.setContentDescription(itemView.getContext().getString(
                    R.string.calificacion_estrellas_descripcion, calificacion.getEstrellas()));
            articulo.setText(itemView.getContext().getString(
                    R.string.calificacion_articulo, calificacion.getTituloArticulo()));

            // El comentario es opcional: sin él no queda un renglón vacío.
            comentario.setVisibility(calificacion.tieneComentario() ? View.VISIBLE : View.GONE);
            comentario.setText(calificacion.getComentario());

            itemView.setOnClickListener(v -> listener.onAutorClick(calificacion.getAutor()));
        }
    }
}
