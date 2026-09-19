package com.example.tpo.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Fila persistida de una pregunta — mapea 1 a 1 con {@link com.example.tpo.model.Pregunta}.
 * PK autogenerada porque, a diferencia de la oferta, acá no hay límite de una
 * fila por usuario y publicación: un mismo interesado puede mandar varias.
 */
@Entity(tableName = "pregunta")
public class PreguntaEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String publicacionId = "";

    @NonNull
    public String autorId = "";

    @NonNull
    public String autorNombre = "";

    @NonNull
    public String texto = "";

    public long fecha;
}
