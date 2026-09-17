package com.example.tpo.model;

import java.io.Serializable;

/**
 * Reputación de un usuario, construida a partir de las calificaciones recibidas
 * <p>
 * El enunciado pide mostrar tres datos: el promedio de estrellas y la cantidad de
 * operaciones concretadas como comprador y como vendedor por separado. Se
 * mantienen separadas y no sumadas porque para el que está por operar no es lo
 * mismo alguien con veinte ventas que alguien con veinte compras.
 * <p>
 * Es un objeto de solo lectura: la reputación la calcula el sistema interno de
 * Ronda a partir de las calificaciones, la app nunca la modifica.
 */
public class Reputacion implements Serializable {

    /** Promedio de estrellas recibidas, de 1 a 5. Vale 0 si todavía no tiene calificaciones. */
    private final double promedioEstrellas;

    /** Cantidad de operaciones concretadas donde el usuario fue el comprador. */
    private final int operacionesComoComprador;

    /** Cantidad de operaciones concretadas donde el usuario fue el vendedor. */
    private final int operacionesComoVendedor;

    public Reputacion(double promedioEstrellas,
                      int operacionesComoComprador,
                      int operacionesComoVendedor) {
        this.promedioEstrellas = promedioEstrellas;
        this.operacionesComoComprador = operacionesComoComprador;
        this.operacionesComoVendedor = operacionesComoVendedor;
    }

    public double getPromedioEstrellas() {
        return promedioEstrellas;
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
     * La pantalla lo necesita para distinguir dos casos que numéricamente son
     * iguales pero significan cosas distintas: un usuario nuevo sin historial no
     * es lo mismo que uno con promedio cero. Sin esto, ambos mostrarían "0,0".
     */
    public boolean tieneCalificaciones() {
        return getTotalOperaciones() > 0;
    }
}
