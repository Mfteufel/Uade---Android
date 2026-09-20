package com.example.tpo.data.local;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.ArrayList;
import java.util.List;

/**
 * Fila de la tabla del borrador de "Publicar artículo" (Punto 5).
 * <p>
 * La tabla tiene siempre a lo sumo una fila: {@link #id} está fijo en
 * {@link #ID_UNICO} y cada guardado la reemplaza entera (ver
 * {@code BorradorPublicacionDao}). El enunciado pide un solo borrador activo por
 * usuario, no un historial de borradores, así que no hace falta más que esto.
 * <p>
 * Las categorías, el estado del artículo y la zona se guardan como el
 * {@code name()} del enum (String) en vez de con un TypeConverter de enum:
 * así, si el día de mañana se elimina o renombra un valor del enum, un borrador
 * viejo en disco no rompe la carga de la fila; en el peor caso ese campo
 * puntual vuelve a quedar sin completar.
 */
@Entity(tableName = "borrador_publicacion")
@TypeConverters(Converters.class)
public class BorradorPublicacionEntity {

    public static final int ID_UNICO = 1;

    @PrimaryKey
    public int id = ID_UNICO;

    @NonNull
    public List<Uri> fotos = new ArrayList<>();

    @NonNull
    public String titulo = "";

    @NonNull
    public String descripcion = "";

    @Nullable
    public String categoria;

    @Nullable
    public String estadoArticulo;

    @Nullable
    public Double precio;

    @Nullable
    public String zona;

    /** Dirección de entrega, texto libre — Punto 8. */
    @Nullable
    public String direccionEntrega;

    public int paso;

    /** Momento del último guardado, en milisegundos. Solo informativo por ahora. */
    public long actualizadoEn;
}
