package com.example.tpo.model;

import java.io.Serializable;

/**
 * Vendedor de una publicación — sus datos públicos para el Punto 4.
 * <p>
 * Modela lo que el enunciado pide mostrar en el Detalle y en el perfil público:
 * nombre, reputación, cantidad de ventas concretadas y desde cuándo es miembro.
 * <p>
 * El {@link #id} es lo que permite decidir "acciones según rol" comparando contra
 * el usuario logueado ({@code SesionUsuario.getIdUsuario()}) sin depender del
 * nombre, que podría repetirse entre personas distintas.
 * <p>
 * Implementa {@link Serializable} igual que {@link Publicacion}, para poder viajar
 * en un Bundle como argumento de navegación.
 */
public class Vendedor implements Serializable {

    private final String id;
    private final String nombre;
    /** Reputación promedio, de 0.0 a 5.0. */
    private final double reputacion;
    /** Operaciones de venta concretadas. */
    private final int cantidadVentas;
    /**
     * Fecha de alta del vendedor en milisegundos desde epoch.
     * <p>
     * Se usa long y no java.time por el mismo motivo que {@link Publicacion}: el
     * minSdk del proyecto es 24 y java.time recién está disponible desde API 26.
     */
    private final long miembroDesde;

    public Vendedor(String id,
                    String nombre,
                    double reputacion,
                    int cantidadVentas,
                    long miembroDesde) {
        this.id = id;
        this.nombre = nombre;
        this.reputacion = reputacion;
        this.cantidadVentas = cantidadVentas;
        this.miembroDesde = miembroDesde;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public double getReputacion() {
        return reputacion;
    }

    public int getCantidadVentas() {
        return cantidadVentas;
    }

    public long getMiembroDesde() {
        return miembroDesde;
    }
}
