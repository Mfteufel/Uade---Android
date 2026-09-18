package com.example.tpo.model;

import java.io.Serializable;
import java.util.UUID;

public class BusquedaGuardada implements Serializable {

    private final String id;
    private final String nombre;
    private final FiltroPublicaciones filtro;
    private final long fechaGuardado;

    public BusquedaGuardada(String nombre, FiltroPublicaciones filtro) {
        this.id = UUID.randomUUID().toString();
        this.nombre = nombre;
        this.filtro = filtro;
        this.fechaGuardado = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public FiltroPublicaciones getFiltro() {
        return filtro;
    }

    public long getFechaGuardado() {
        return fechaGuardado;
    }
}
