package com.example.tpo.model;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Una publicación del listado del Home y del Detalle.
 * <p>
 * Modela título, precio, estado, zona, categoría, descripción, fecha, vendedor
 * y cantidad de fotos. El vendedor es un {@link Vendedor} (no un String suelto):
 * así el Detalle puede mostrar su reputación y abrir su perfil público, y decidir
 * las "acciones según rol" comparando por id contra el usuario logueado.
 * <p>
 * Implementa {@link Serializable} para poder viajar en un Bundle como argumento
 * de navegación (por ejemplo, con el filtro del Home hacia el bottom sheet).
 */
public class Publicacion implements Serializable {

    private final String id;
    private final String titulo;
    private final String descripcion;
    /**
     * Precio en pesos. Se usa double por simplicidad del TPO; si el backend
     * devolviera centavos convendría un long para evitar errores de redondeo.
     * No es final: ver {@link #actualizarPrecio}.
     */
    private double precio;
    private final EstadoArticulo estado;
    private final Categoria categoria;
    private final Zona zona;
    /**
     * Fecha de publicación en milisegundos desde epoch.
     * <p>
     * Se usa long y no java.time.LocalDate porque el minSdk del proyecto es 24 y
     * java.time recién está disponible desde API 26 (haría falta desugaring).
     */
    private final long fechaPublicacion;
    private final Vendedor vendedor;
    /**
     * Cantidad de fotos de la publicación, para la galería del Detalle.
     * <p>
     * Sigue existiendo para el catálogo de prueba, que no tiene fotos reales:
     * la galería muestra este número de placeholders (ver {@code ic_imagen}).
     * Contra el backend real, {@link #getFotos()} trae las URLs de verdad y es
     * lo que usa la galería cuando no está vacía (ver {@code DetalleFragment}).
     */
    private final int cantidadFotos;
    /**
     * Estado de la publicación (activa / pausada / vendida) — Punto 4,
     * "gestión de la publicación".
     * <p>
     * Es el único campo no {@code final} de la clase: todo lo demás describe el
     * artículo tal como se publicó y no cambia, pero el estado es justamente lo
     * que el vendedor puede modificar después de publicar (pausar, reactivar,
     * marcar como vendida). Contra la API real este campo llegaría en el
     * {@code GET} y se actualizaría con un {@code PUT
     * /publicaciones/{id}/estado}; acá lo muta directamente el repositorio mock.
     * No se agrega al constructor a propósito: todas las publicaciones del
     * catálogo de prueba arrancan {@link EstadoPublicacion#ACTIVA}.
     */
    private EstadoPublicacion estadoPublicacion = EstadoPublicacion.ACTIVA;
    /**
     * Dirección exacta del punto de entrega, en texto libre (puede ser una
     * dirección o directamente coordenadas pegadas de Google Maps) — cargada
     * por el vendedor al publicar (Punto 5). El backend real solo la manda acá
     * cuando quien pide el detalle es el propio dueño de la publicación
     * (ver {@code routers/publicaciones.py}); el comprador la recibe por otro
     * lado, en su {@code OfertaNegociacion}, recién cuando esa oferta queda
     * {@code ACEPTADA} — esta clase no impone esa regla, solo guarda el dato
     * tal como llegó. La UI que decide mostrarlo o no vive en el Detalle de
     * Publicación (Punto 8).
     * <p>
     * {@code null} si quien pregunta no es el dueño, o si la publicación
     * todavía no tiene dirección cargada.
     */
    @Nullable
    private final String direccionEntrega;

    /**
     * URL de la foto principal, contra la API real. No va en el constructor a
     * propósito, mismo criterio que {@link #estadoPublicacion}: el catálogo de
     * prueba ({@code PublicacionRepositoryMock}) no tiene fotos reales y sigue
     * mostrando {@code cantidadFotos} placeholders; las respuestas del backend
     * ({@code PublicacionResumenResponse}/{@code PublicacionDetalleResponse})
     * la completan después de construir el objeto.
     */
    @Nullable
    private String fotoPrincipalUrl;

    /** URLs de toda la galería, para el Detalle. Vacía si no hay fotos reales (ver {@link #fotoPrincipalUrl}). */
    private List<String> fotos = Collections.emptyList();

    public Publicacion(String id,
                       String titulo,
                       String descripcion,
                       double precio,
                       EstadoArticulo estado,
                       Categoria categoria,
                       Zona zona,
                       long fechaPublicacion,
                       Vendedor vendedor,
                       int cantidadFotos,
                       @Nullable String direccionEntrega) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.precio = precio;
        this.estado = estado;
        this.categoria = categoria;
        this.zona = zona;
        this.fechaPublicacion = fechaPublicacion;
        this.vendedor = vendedor;
        this.cantidadFotos = cantidadFotos;
        this.direccionEntrega = direccionEntrega;
    }

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    /**
     * Campo mutable de la clase: se usa para
     * simular una baja de precio (Punto 10, indicador de novedad) para
     * que el cambio se vea en cualquier pantalla sin recargar desde el repositorio.
     */
    public void actualizarPrecio(double nuevoPrecio) {
        this.precio = nuevoPrecio;
    }

    public EstadoArticulo getEstado() {
        return estado;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Zona getZona() {
        return zona;
    }

    public long getFechaPublicacion() {
        return fechaPublicacion;
    }

    public Vendedor getVendedor() {
        return vendedor;
    }

    public int getCantidadFotos() {
        return cantidadFotos;
    }

    public EstadoPublicacion getEstadoPublicacion() {
        return estadoPublicacion;
    }

    public void setEstadoPublicacion(EstadoPublicacion estadoPublicacion) {
        this.estadoPublicacion = estadoPublicacion;
    }

    @Nullable
    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    @Nullable
    public String getFotoPrincipalUrl() {
        return fotoPrincipalUrl;
    }

    public void setFotoPrincipalUrl(@Nullable String fotoPrincipalUrl) {
        this.fotoPrincipalUrl = fotoPrincipalUrl;
    }

    public List<String> getFotos() {
        return fotos;
    }

    public void setFotos(@Nullable List<String> fotos) {
        this.fotos = fotos != null ? fotos : Collections.emptyList();
    }
}
