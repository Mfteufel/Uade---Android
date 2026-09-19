package com.example.tpo.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * Una publicación que un usuario guardó — Punto 4 ("guardar" en el Detalle).
 * <p>
 * Clave compuesta ({@code usuarioId}, {@code publicacionId}) en vez de un id
 * propio: no hace falta más que eso para saber si está guardada, y evita
 * duplicados sin lógica extra (guardar dos veces la misma publicación
 * simplemente reemplaza la fila).
 */
@Entity(tableName = "publicacion_guardada", primaryKeys = {"usuarioId", "publicacionId"})
public class PublicacionGuardadaEntity {

    @NonNull
    public String usuarioId = "";

    @NonNull
    public String publicacionId = "";

    /** Momento en que se guardó, para poder ordenar "la última arriba". */
    public long fechaGuardado;
}
