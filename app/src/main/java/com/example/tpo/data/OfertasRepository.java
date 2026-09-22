package com.example.tpo.data;

import androidx.annotation.Nullable;

import com.example.tpo.model.OfertaNegociacion;

import java.util.List;

/**
 * Negociación de ofertas — "Mis ofertas" (Punto 7), tal cual el contrato de
 * {@code docs/ofertas-api.md}.
 * <p>
 * Dos implementaciones detrás de esta interfaz, elegidas en un solo lugar
 * ({@code di/RepositoryModule}): {@link OfertasRepositoryMock} (datos falsos,
 * mientras no existe el backend) y {@link OfertasRepositoryRemoto} (Retrofit,
 * Fase 4).
 * <p>
 * A propósito no tiene nada que ver con {@code OfertasPublicacion} (Room), que
 * sigue siendo lo que usa el botón "ofertar" del Detalle de publicación (Punto
 * 4, pantalla de otro compañero): son dos sistemas de ofertas que conviven sin
 * pisarse hasta que se decida migrar el Detalle a este contrato.
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
