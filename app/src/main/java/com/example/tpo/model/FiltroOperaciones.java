package com.example.tpo.model;

import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Criterios del historial de operaciones (Punto 9): tipo y rango de fechas.
 * <p>
 * Misma idea que {@link FiltroPublicaciones}: todo lo que define "qué lista se
 * muestra" viaja en un solo objeto, así el Fragment tiene un único método de
 * recarga y el día que exista la API se traduce directo a query params
 * ({@code ?tipo=&desde=&hasta=}).
 */
public class FiltroOperaciones implements Serializable {

    /** {@code null} = compras y ventas juntas. */
    @Nullable
    private TipoOperacion tipo = null;

    /**
     * Extremos del rango, en milisegundos desde epoch e inclusivos. {@code null} =
     * sin límite de ese lado. Se comparan contra la fecha de entrega, que es
     * cuando la operación se concretó.
     */
    @Nullable
    private Long desde = null;
    @Nullable
    private Long hasta = null;

    @Nullable
    public TipoOperacion getTipo() {
        return tipo;
    }

    public void setTipo(@Nullable TipoOperacion tipo) {
        this.tipo = tipo;
    }

    @Nullable
    public Long getDesde() {
        return desde;
    }

    @Nullable
    public Long getHasta() {
        return hasta;
    }

    public void setRangoFechas(@Nullable Long desde, @Nullable Long hasta) {
        this.desde = desde;
        this.hasta = hasta;
    }

    public boolean tieneRangoFechas() {
        return desde != null || hasta != null;
    }

    /** true si la operación entra en el filtro. Lo usa el repositorio mock (y lo haría el servidor). */
    public boolean incluye(Operacion operacion) {
        if (tipo != null && operacion.getTipo() != tipo) {
            return false;
        }
        long fecha = operacion.getFechaReferencia();
        if (desde != null && fecha < desde) {
            return false;
        }
        return hasta == null || fecha <= hasta;
    }
}
