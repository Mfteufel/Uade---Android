package com.example.tpo.model;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.example.tpo.util.MapaUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Datos que el usuario va completando en el wizard de "Publicar artículo" (Punto 5).
 * <p>
 * Es el objeto que vive en {@link com.example.tpo.ui.publicar.PublicarArticuloViewModel}
 * mientras se recorren los pasos, y también el que se persiste en Room
 * ({@code BorradorPublicacionEntity}) para poder retomarlo si el usuario cierra la
 * app a mitad de carga. A diferencia de {@link Publicacion} (que es inmutable,
 * porque representa algo que ya se publicó) este objeto es mutable a propósito:
 * cada paso del wizard modifica el mismo borrador en vez de crear uno nuevo.
 */
public class BorradorPublicacion {

    /** Fotos elegidas de la galería, todavía no subidas. */
    private List<Uri> fotos = new ArrayList<>();

    private String titulo = "";
    private String descripcion = "";

    @Nullable
    private Categoria categoria;
    @Nullable
    private EstadoArticulo estadoArticulo;
    @Nullable
    private Double precio;
    @Nullable
    private Zona zona;

    /**
     * Dirección exacta del punto de entrega, en texto libre (dirección o
     * coordenadas pegadas de Google Maps) — Punto 8. Se le muestra al
     * comprador recién cuando el vendedor acepta su oferta (ver
     * {@code OfertasRepository}/{@code MapaUtils}); acá solo se captura y
     * se guarda, la regla de cuándo mostrarla vive en el Detalle.
     */
    @Nullable
    private String direccionEntrega;

    /**
     * Último paso del wizard en el que estuvo el usuario (0 = fotos). Es lo que
     * permite reabrir el wizard justo donde lo dejó y no siempre desde el paso 1.
     */
    private int paso = 0;

    public List<Uri> getFotos() {
        return fotos;
    }

    public void setFotos(List<Uri> fotos) {
        this.fotos = fotos != null ? fotos : new ArrayList<>();
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo == null ? "" : titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion == null ? "" : descripcion;
    }

    @Nullable
    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(@Nullable Categoria categoria) {
        this.categoria = categoria;
    }

    @Nullable
    public EstadoArticulo getEstadoArticulo() {
        return estadoArticulo;
    }

    public void setEstadoArticulo(@Nullable EstadoArticulo estadoArticulo) {
        this.estadoArticulo = estadoArticulo;
    }

    @Nullable
    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(@Nullable Double precio) {
        this.precio = precio;
    }

    @Nullable
    public Zona getZona() {
        return zona;
    }

    public void setZona(@Nullable Zona zona) {
        this.zona = zona;
    }

    @Nullable
    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(@Nullable String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public int getPaso() {
        return paso;
    }

    public void setPaso(int paso) {
        this.paso = paso;
    }

    /** true si hay al menos un dato cargado. Se usa para no guardar un borrador vacío apenas se abre el wizard. */
    public boolean tieneDatos() {
        return !fotos.isEmpty() || !titulo.isEmpty() || !descripcion.isEmpty()
                || categoria != null || estadoArticulo != null || precio != null || zona != null
                || MapaUtils.tieneDireccion(direccionEntrega);
    }

    /** true si están completos los datos obligatorios para poder publicar. */
    public boolean estaCompleto() {
        return !fotos.isEmpty()
                && !titulo.trim().isEmpty()
                && !descripcion.trim().isEmpty()
                && categoria != null
                && estadoArticulo != null
                && precio != null && precio > 0
                && zona != null
                && MapaUtils.tieneDireccion(direccionEntrega);
    }
}
