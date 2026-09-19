package com.example.tpo.data.local;

import android.net.Uri;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * TypeConverters de Room para columnas que no son tipos primitivos.
 * <p>
 * Room solo entiende columnas de tipos simples; para guardar la lista de fotos
 * del borrador (cada una una {@link Uri} de la galería) hay que decirle cómo
 * convertirla a String y viceversa. Se usa Gson porque ya es una dependencia del
 * proyecto (la trae Retrofit), así se evita sumar otra librería solo para esto.
 */
public class Converters {

    private static final Gson GSON = new Gson();
    private static final Type TIPO_LISTA_STRING = new TypeToken<List<String>>() {}.getType();

    @TypeConverter
    public static String desdeListaUris(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) {
            return "";
        }
        List<String> textos = new ArrayList<>();
        for (Uri uri : uris) {
            textos.add(uri.toString());
        }
        return GSON.toJson(textos);
    }

    @TypeConverter
    public static List<Uri> haciaListaUris(String json) {
        List<Uri> uris = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return uris;
        }
        List<String> textos = GSON.fromJson(json, TIPO_LISTA_STRING);
        if (textos != null) {
            for (String texto : textos) {
                uris.add(Uri.parse(texto));
            }
        }
        return uris;
    }
}
