package com.example.tpo.data;

import androidx.annotation.Nullable;

import com.example.tpo.model.OfertaNegociacion;

import java.util.List;

/**
 * Negociación de ofertas — "Mis ofertas" (Punto 7) y también "ofertar"/"mi
 * oferta"/"ofertas recibidas" del Detalle de publicación (Punto 4), tal cual
 * el contrato de {@code docs/ofertas-api.md}.
 * <p>
 * Única implementación: {@link OfertasRepositoryRemoto} (Retrofit), provista
 * sin flag por {@code di/RepositoryModule} porque el backend de ofertas ya
 * existe desde el día uno. El Detalle usaba antes {@code OfertasPublicacion}
 * (Room, ya eliminada) para "mi oferta" y "ofertas recibidas"; ahora los dos
 * puntos comparten este mismo contrato.
 */
public interface OfertasRepository {

    /** Crea una oferta nueva sobre una publicación ajena. */
    void crear(String publicacionId, double precio, @Nullable String mensaje,
              RepositorioCallback<OfertaNegociacion> callback);

    /** Ofertas que hice como comprador. */
    void enviadas(RepositorioCallback<List<OfertaNegociacion>> callback);

    /** Ofertas que recibí como vendedor. */
    void recibidas(RepositorioCallback<List<OfertaNegociacion>> callback);

    /** Una oferta puntual, con su estado más actualizado. */
    void obtener(String ofertaId, RepositorioCallback<OfertaNegociacion> callback);

    /** Acepta la oferta: la marca ACEPTADA y la publicación pasa a VENDIDA. */
    void aceptar(String ofertaId, RepositorioCallback<OfertaNegociacion> callback);

    /** Rechaza la oferta: pasa a RECHAZADA, terminal. */
    void rechazar(String ofertaId, RepositorioCallback<OfertaNegociacion> callback);

    /** Contraoferta: nuevo precio, pasa el turno a la otra parte y renueva el vencimiento. */
    void contraofertar(String ofertaId, double precio, RepositorioCallback<OfertaNegociacion> callback);
}
