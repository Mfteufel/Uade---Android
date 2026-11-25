package com.example.tpo.service;

/**
 * A dónde lleva tocar un aviso (Punto 11): la publicación o la oferta que lo originó.
 * <p>
 * Es Java puro, sin {@code Intent} ni {@code NavController}: {@code MainActivity} le pasa lo que
 * leyó de los extras de la notificación y recibe el destino ya decidido. Así la regla se puede
 * probar con un test unitario común.
 * <p>
 * Los tipos son los que manda el backend (backend/avisos.py):
 * <ul>
 *     <li>{@code PREGUNTA_NUEVA}, {@code PREGUNTA_RESPONDIDA} → la publicación.</li>
 *     <li>{@code OFERTA_RECIBIDA}, {@code OFERTA_ACEPTADA}, {@code OFERTA_RECHAZADA},
 *     {@code CONTRAOFERTA} → la oferta.</li>
 * </ul>
 * Si el aviso no trae lo necesario para abrir la oferta pero sí la publicación, se abre la
 * publicación en vez de no hacer nada. Si no trae ni una cosa ni la otra no hay destino.
 */
public final class AvisoDestino {

    public enum Tipo {
        PUBLICACION,
        OFERTA
    }

    private static final String PREFIJO_OFERTA = "OFERTA_";
    private static final String TIPO_CONTRAOFERTA = "CONTRAOFERTA";
    private static final String ROL_VENDEDOR = "VENDEDOR";

    private final Tipo tipo;
    private final String publicacionId;
    private final String ofertaId;
    private final boolean comoVendedor;

    private AvisoDestino(Tipo tipo, String publicacionId, String ofertaId, boolean comoVendedor) {
        this.tipo = tipo;
        this.publicacionId = publicacionId;
        this.ofertaId = ofertaId;
        this.comoVendedor = comoVendedor;
    }

    /**
     * @param tipoAviso     {@code tipo} de la novedad
     * @param publicacionId id de la publicación, puede ser null o vacío
     * @param ofertaId      id de la oferta, puede ser null o vacío
     * @param rol           rol del que recibe el aviso en la oferta ({@code VENDEDOR} o {@code COMPRADOR})
     * @return el destino, o null si el aviso no alcanza para abrir nada
     */
    public static AvisoDestino desde(String tipoAviso, String publicacionId, String ofertaId, String rol) {
        String publicacion = limpiar(publicacionId);
        String oferta = limpiar(ofertaId);

        if (esDeOferta(tipoAviso) && oferta != null) {
            return new AvisoDestino(Tipo.OFERTA, publicacion, oferta, ROL_VENDEDOR.equalsIgnoreCase(limpiar(rol)));
        }
        if (publicacion != null) {
            return new AvisoDestino(Tipo.PUBLICACION, publicacion, null, false);
        }
        return null;
    }

    private static boolean esDeOferta(String tipoAviso) {
        return tipoAviso != null
                && (tipoAviso.startsWith(PREFIJO_OFERTA) || tipoAviso.equals(TIPO_CONTRAOFERTA));
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** Siempre viene para {@link Tipo#PUBLICACION}; para {@link Tipo#OFERTA} puede ser null. */
    public String getPublicacionId() {
        return publicacionId;
    }

    /** Solo viene para {@link Tipo#OFERTA}. */
    public String getOfertaId() {
        return ofertaId;
    }

    /**
     * true si el que recibió el aviso es el vendedor de la oferta: la oferta está en la tab
     * "Recibidas" de Mis ofertas. Si no, está en "Enviadas".
     */
    public boolean esComoVendedor() {
        return comoVendedor;
    }
}
