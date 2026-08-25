package com.example.tpo.model;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

/**
 * Criterios de búsqueda del Home: texto libre + filtros combinados + ordenamiento.
 * <p>
 * Junta en un solo objeto todo lo que define "qué lista se muestra". Eso permite
 * que el Fragment tenga un único método de recarga en vez de una rama por filtro,
 * y que el día que exista la API se mande este objeto entero como query params.
 * <p>
 * El ordenamiento no es estrictamente un filtro, pero viaja acá porque forma parte
 * de la misma consulta al repositorio.
 * <p>
 * Es {@link Serializable} para poder mandarlo y recibirlo en un Bundle entre el
 * Home y el bottom sheet de filtros.
 */
public class FiltroPublicaciones implements Serializable {

    /** Búsqueda por texto libre sobre título y descripción. Cadena vacía = sin búsqueda. */
    private String texto = "";

    /** Categoría seleccionada. {@code null} significa "todas las categorías". */
    @Nullable
    private Categoria categoria = null;

    /**
     * Estados aceptados. Set vacío = no filtrar por estado (se muestran todos).
     * Es un Set porque el enunciado pide filtros combinados: el usuario puede
     * querer ver "nuevo" y "como nuevo" a la vez.
     */
    private final Set<EstadoArticulo> estados = EnumSet.noneOf(EstadoArticulo.class);

    /** Extremos del rango de precio. {@code null} = sin límite de ese lado. */
    @Nullable
    private Double precioMinimo = null;
    @Nullable
    private Double precioMaximo = null;

    private Cercania cercania = Cercania.TODAS;

    private OrdenPublicaciones orden = OrdenPublicaciones.RECIENTES;

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto == null ? "" : texto;
    }

    @Nullable
    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(@Nullable Categoria categoria) {
        this.categoria = categoria;
    }

    public Set<EstadoArticulo> getEstados() {
        return estados;
    }

    public void setEstados(Set<EstadoArticulo> nuevos) {
        estados.clear();
        if (nuevos != null) {
            estados.addAll(nuevos);
        }
    }

    @Nullable
    public Double getPrecioMinimo() {
        return precioMinimo;
    }

    public void setPrecioMinimo(@Nullable Double precioMinimo) {
        this.precioMinimo = precioMinimo;
    }

    @Nullable
    public Double getPrecioMaximo() {
        return precioMaximo;
    }

    public void setPrecioMaximo(@Nullable Double precioMaximo) {
        this.precioMaximo = precioMaximo;
    }

    public Cercania getCercania() {
        return cercania;
    }

    public void setCercania(Cercania cercania) {
        this.cercania = cercania == null ? Cercania.TODAS : cercania;
    }

    public OrdenPublicaciones getOrden() {
        return orden;
    }

    public void setOrden(OrdenPublicaciones orden) {
        this.orden = orden == null ? OrdenPublicaciones.RECIENTES : orden;
    }

    /**
     * Cantidad de filtros del bottom sheet que están activos (precio, estado y
     * cercanía). Se usa para mostrar el contador en el botón "Filtros".
     * <p>
     * No cuenta ni el texto ni la categoría porque esos dos tienen su propio
     * control visible en pantalla y el usuario ya los ve aplicados.
     */
    public int contarFiltrosAvanzadosActivos() {
        int activos = 0;
        if (!estados.isEmpty()) {
            activos++;
        }
        if (precioMinimo != null || precioMaximo != null) {
            activos++;
        }
        if (cercania != Cercania.TODAS) {
            activos++;
        }
        return activos;
    }

    /** Deja solo los filtros del bottom sheet en su valor por defecto. */
    public void limpiarFiltrosAvanzados() {
        estados.clear();
        precioMinimo = null;
        precioMaximo = null;
        cercania = Cercania.TODAS;
    }

    /**
     * Devuelve una copia independiente del filtro.
     * <p>
     * El bottom sheet edita una copia y no el filtro real: así, si el usuario
     * toca los controles y después cierra el sheet sin aplicar, el listado del
     * Home queda intacto.
     */
    public FiltroPublicaciones copia() {
        FiltroPublicaciones copia = new FiltroPublicaciones();
        copia.texto = this.texto;
        copia.categoria = this.categoria;
        copia.estados.addAll(this.estados);
        copia.precioMinimo = this.precioMinimo;
        copia.precioMaximo = this.precioMaximo;
        copia.cercania = this.cercania;
        copia.orden = this.orden;
        return copia;
    }
}
