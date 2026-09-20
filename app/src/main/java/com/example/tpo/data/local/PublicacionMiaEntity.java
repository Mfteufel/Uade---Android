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
 * Fila de "Mis publicaciones" (Punto 5), persistida localmente con Room mientras
 * no exista un backend real. A diferencia de {@link BorradorPublicacionEntity}
 * (una sola fila) acá hay una fila por publicación, filtrable por
 * {@link #vendedorId} para que cada usuario vea solo lo suyo al cambiar de
 * sesión en el mismo dispositivo.
 * <p>
 * El día que exista el backend de la cátedra, esta tabla se reemplaza por
 * {@code MisPublicacionesRepositoryApi} (ya escrita, con Retrofit) sin tocar la
 * interfaz {@code MisPublicacionesRepository} ni las pantallas.
 */
@Entity(tableName = "mi_publicacion")
@TypeConverters(Converters.class)
public class PublicacionMiaEntity {

    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String vendedorId = "";

    @NonNull
    public String titulo = "";

    @NonNull
    public String descripcion = "";

    @Nullable
    public String categoria;

    @Nullable
    public String estadoArticulo;

    public double precio;

    @Nullable
    public String zona;

    @NonNull
    public List<Uri> fotos = new ArrayList<>();

    @NonNull
    public String estadoPublicacion = "ACTIVA";

    public long fechaPublicacion;
}
