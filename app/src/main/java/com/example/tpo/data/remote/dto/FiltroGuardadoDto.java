package com.example.tpo.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class FiltroGuardadoDto {

    @Nullable
    @SerializedName("texto")
    public String texto;

    @Nullable
    @SerializedName("categoria")
    public String categoria;

    @SerializedName("estados")
    public List<String> estados = new ArrayList<>();

    @SerializedName("zonas")
    public List<String> zonas = new ArrayList<>();

    @Nullable
    @SerializedName("precioMinimo")
    public Double precioMinimo;

    @Nullable
    @SerializedName("precioMaximo")
    public Double precioMaximo;

    @SerializedName("orden")
    public String orden;
}
