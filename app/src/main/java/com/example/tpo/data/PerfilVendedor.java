package com.example.tpo.data;

import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Vendedor;

import java.util.List;

/**
 * Resultado de pedir el perfil público de un vendedor (Punto 4): sus datos y sus
 * publicaciones activas.
 * <p>
 * Los dos vienen juntos en una sola respuesta a propósito: la pantalla los
 * necesita a la vez, así hay un solo estado de carga y un solo camino de error.
 * Contra la API real esto es un {@code GET /vendedores/{id}/perfil}.
 */
public class PerfilVendedor {

    private final Vendedor vendedor;
    private final List<Publicacion> publicaciones;

    public PerfilVendedor(Vendedor vendedor, List<Publicacion> publicaciones) {
        this.vendedor = vendedor;
        this.publicaciones = publicaciones;
    }

    public Vendedor getVendedor() {
        return vendedor;
    }

    public List<Publicacion> getPublicaciones() {
        return publicaciones;
    }
}
