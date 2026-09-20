package com.example.tpo.model;

import java.io.Serializable;

/**
 * Reputación de un usuario, construida a partir de las calificaciones recibidas
 * y de las operaciones concretadas (Puntos 2 y 9 del TPO).
 * <p>
 * El enunciado pide mostrar el promedio de estrellas y la cantidad de operaciones
 * concretadas como comprador y como vendedor por separado. Se mantienen separadas
 * y no sumadas porque para el que está por operar no es lo mismo alguien con
 * veinte ventas que alguien con veinte compras.
 * <p>
 * Es un objeto de solo lectura: la reputación la calcula el servidor a partir de
 * las tablas de operaciones y calificaciones (hoy lo simula
 * {@code BaseDeDatosMock}); la app nunca la calcula ni la modifica, solo la muestra.
 */
public class Reputacion implements Serializable {

    /** Promedio de estrellas recibidas, de 1 a 5. Vale 0 si todavía no tiene calificaciones. */
    private final double promedioEstrellas;

    /**
     * Cantidad de calificaciones recibidas. Es lo que define si el promedio
     * significa algo: 0 calificaciones y promedio 0 no es "mala reputación".
     */
    private final int cantidadCalificaciones;

    /** Cantidad de operaciones concretadas donde el usuario fue el comprador. */
    private final int operacionesComoComprador;

    /** Cantidad de operaciones concretadas donde el usuario fue el vendedor. */
    private final int operacionesComoVendedor;

    public Reputacion(double promedioEstrellas,
                      int cantidadCalificaciones,
                      int operacionesComoComprador,
                      int operacionesComoVendedor) {
        this.promedioEstrellas = promedioEstrellas;
        this.cantidadCalificaciones = cantidadCalificaciones;
        this.operacionesComoComprador = operacionesComoComprador;
        this.operacionesComoVendedor = operacionesComoVendedor;
    }

    public double getPromedioEstrellas() {
        return promedioEstrellas;
    }

    public int getCantidadCalificaciones() {
        return cantidadCalificaciones;
    }

    public int getOperacionesComoComprador() {
        return operacionesComoComprador;
    }

    public int getOperacionesComoVendedor() {
        return operacionesComoVendedor;
    }

    /** Total de operaciones concretadas, sin distinguir el rol. */
    public int getTotalOperaciones() {
        return operacionesComoComprador + operacionesComoVendedor;
    }

    /**
     * true si el usuario ya recibió al menos una calificación.
     * <p>
     * La pantalla lo necesita para distinguir un usuario sin calificaciones de uno
     * con promedio bajo. Se mira la cantidad de calificaciones y no la de
     * operaciones: alguien puede haber concretado operaciones que todavía nadie
     * calificó, y en ese caso mostrar "0,0 ★" sería mentirle al que mira.
     */
    public boolean tieneCalificaciones() {
        return cantidadCalificaciones > 0;
    }
}
