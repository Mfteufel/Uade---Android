package com.example.tpo.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Body del POST que marca una publicación como favorita. */
public class FavoritoNuevoRequest {

    @SerializedName("publicacionId")
    public final String publicacionId;

    public FavoritoNuevoRequest(String publicacionId) {
        this.publicacionId = publicacionId;
    }
}
