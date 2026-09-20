package com.example.tpo.ui.historial;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.model.Operacion;
import com.example.tpo.model.TipoOperacion;
import com.example.tpo.model.UsuarioResumen;
import com.example.tpo.util.FormatoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter del historial de operaciones (Punto 9).
 * <p>
 * Tiene dos tipos de fila: encabezados de sección ("Compras", "Ventas") y
 * operaciones. Con el filtro en "Todas" el historial se muestra separado en las
 * dos secciones, que es lo que pide el enunciado; con un tipo elegido, el
 * encabezado sobra y no se agrega.
 * <p>
 * Como los otros adapters de la app, no navega ni decide nada: avisa al Fragment.
 */
public class OperacionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Avisos al Fragment. */
    public interface Listener {
        /** Se tocó "Calificar" en una operación calificable. */
        void onCalificarClick(Operacion operacion);

        /** Se tocó la contraparte: abrir su perfil público. */
        void onContraparteClick(UsuarioResumen contraparte);
    }

    private static final int TIPO_ENCABEZADO = 0;
    private static final int TIPO_OPERACION = 1;

    /** Cada fila es un {@link TipoOperacion} (encabezado) o una {@link Operacion}. */
    private final List<Object> filas = new ArrayList<>();
    private final Listener listener;

    public OperacionAdapter(Listener listener) {
        this.listener = listener;
    }

    /**
     * Reemplaza el contenido.
     *
     * @param separarPorTipo true para agrupar en "Compras" y "Ventas" con sus
     *                       encabezados (filtro "Todas"). Dentro de cada grupo se
     *                       respeta el orden recibido (más recientes primero).
     */
    public void mostrar(List<Operacion> operaciones, boolean separarPorTipo) {
        filas.clear();
        if (separarPorTipo) {
            agregarGrupo(operaciones, TipoOperacion.COMPRA);
            agregarGrupo(operaciones, TipoOperacion.VENTA);
        } else {
            filas.addAll(operaciones);
        }
        notifyDataSetChanged();
    }

    private void agregarGrupo(List<Operacion> operaciones, TipoOperacion tipo) {
        List<Operacion> grupo = new ArrayList<>();
        for (Operacion operacion : operaciones) {
            if (operacion.getTipo() == tipo) {
                grupo.add(operacion);
            }
        }
        if (!grupo.isEmpty()) {
            filas.add(tipo);
            filas.addAll(grupo);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return filas.get(position) instanceof TipoOperacion ? TIPO_ENCABEZADO : TIPO_OPERACION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_ENCABEZADO) {
            return new EncabezadoViewHolder(
                    inflater.inflate(R.layout.item_encabezado_seccion, parent, false));
        }
        return new OperacionViewHolder(inflater.inflate(R.layout.item_operacion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object fila = filas.get(position);
        if (holder instanceof EncabezadoViewHolder) {
            ((EncabezadoViewHolder) holder).enlazar((TipoOperacion) fila);
        } else {
            ((OperacionViewHolder) holder).enlazar((Operacion) fila, listener);
        }
    }

    @Override
    public int getItemCount() {
        return filas.size();
    }

    static class EncabezadoViewHolder extends RecyclerView.ViewHolder {

        private final TextView texto;

        EncabezadoViewHolder(@NonNull View vista) {
            super(vista);
            texto = vista.findViewById(R.id.textoEncabezadoSeccion);
        }

        void enlazar(TipoOperacion tipo) {
            texto.setText(tipo.getEtiqueta());
        }
    }

    static class OperacionViewHolder extends RecyclerView.ViewHolder {

        private final TextView articulo;
        private final TextView monto;
        private final TextView contraparte;
        private final TextView fecha;
        private final View contenedorCalificable;
        private final TextView plazo;
        private final View botonCalificar;
        private final View contenedorCalificado;
        private final RatingBar miCalificacion;
        private final View plazoVencido;

        OperacionViewHolder(@NonNull View vista) {
            super(vista);
            articulo = vista.findViewById(R.id.textoArticuloOperacion);
            monto = vista.findViewById(R.id.textoMontoOperacion);
            contraparte = vista.findViewById(R.id.textoContraparteOperacion);
            fecha = vista.findViewById(R.id.textoFechaOperacion);
            contenedorCalificable = vista.findViewById(R.id.contenedorCalificable);
            plazo = vista.findViewById(R.id.textoPlazoCalificacion);
            botonCalificar = vista.findViewById(R.id.botonCalificar);
            contenedorCalificado = vista.findViewById(R.id.contenedorCalificado);
            miCalificacion = vista.findViewById(R.id.barraMiCalificacion);
            plazoVencido = vista.findViewById(R.id.textoPlazoVencido);
        }

        void enlazar(Operacion operacion, Listener listener) {
            Context context = itemView.getContext();

            articulo.setText(operacion.getTituloArticulo());
            monto.setText(FormatoUtils.precio(operacion.getMontoFinal()));
            int textoContraparte = operacion.getTipo() == TipoOperacion.COMPRA
                    ? R.string.historial_le_compraste : R.string.historial_le_vendiste;
            contraparte.setText(context.getString(textoContraparte,
                    operacion.getContraparte().getNombre()));
            contraparte.setOnClickListener(v -> listener.onContraparteClick(operacion.getContraparte()));
            fecha.setText(context.getString(R.string.historial_entregada_el,
                    FormatoUtils.fechaCompleta(operacion.getFechaReferencia())));

            // Qué mostrar lo decidió el servidor; acá solo se refleja.
            boolean calificable = operacion.puedeCalificar();
            boolean calificada = operacion.yaCalifique();
            contenedorCalificable.setVisibility(calificable ? View.VISIBLE : View.GONE);
            contenedorCalificado.setVisibility(calificada ? View.VISIBLE : View.GONE);
            plazoVencido.setVisibility(!calificable && !calificada ? View.VISIBLE : View.GONE);

            if (calificable && operacion.getCalificableHasta() != null) {
                plazo.setText(context.getString(R.string.historial_calificar_hasta,
                        FormatoUtils.fechaCorta(operacion.getCalificableHasta())));
            }
            if (calificada) {
                int estrellas = operacion.getMiCalificacion().getEstrellas();
                miCalificacion.setRating(estrellas);
                miCalificacion.setContentDescription(context.getString(
                        R.string.calificacion_estrellas_descripcion, estrellas));
            }
            botonCalificar.setOnClickListener(v -> listener.onCalificarClick(operacion));
        }
    }
}
