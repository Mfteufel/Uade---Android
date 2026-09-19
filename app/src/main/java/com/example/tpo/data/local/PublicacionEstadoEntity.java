package com.example.tpo.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Override persistido del estado de una publicación (pausada/vendida/reactivada),
 * Punto 4. No es el catálogo completo — ese sigue siendo el mock en memoria de
 * {@code PublicacionRepositoryMock}, que ya trae un estado por defecto
 * ({@code ACTIVA}) para cada publicación. Esta tabla solo guarda las que el
 * vendedor cambió a mano, para volver a aplicarlas sobre el catálogo cuando el
 * proceso arranca de nuevo.
 */
@Entity(tableName = "publicacion_estado")
public class PublicacionEstadoEntity {

    @PrimaryKey
    @NonNull
    public String publicacionId = "";

    @NonNull
    public String estado = "ACTIVA";
}
