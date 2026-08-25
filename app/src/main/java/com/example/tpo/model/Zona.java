package com.example.tpo.model;

/**
 * Zona (barrio / localidad) donde el vendedor entrega el artículo en mano.
 * <p>
 * Cada zona pertenece a una {@link Region}. Esa agrupación es la que nos permite
 * resolver el filtro de "cercanía a la zona del usuario" que pide el Punto 3 sin
 * necesidad de GPS ni de calcular distancias reales: se considera cercana toda
 * zona que esté en la misma región que la zona del usuario logueado.
 */
public enum Zona {

    PALERMO("Palermo", Region.CABA_NORTE),
    BELGRANO("Belgrano", Region.CABA_NORTE),
    NUNEZ("Núñez", Region.CABA_NORTE),
    RECOLETA("Recoleta", Region.CABA_NORTE),

    CABALLITO("Caballito", Region.CABA_CENTRO),
    ALMAGRO("Almagro", Region.CABA_CENTRO),
    VILLA_CRESPO("Villa Crespo", Region.CABA_CENTRO),

    FLORES("Flores", Region.CABA_SUR),
    BOEDO("Boedo", Region.CABA_SUR),
    BARRACAS("Barracas", Region.CABA_SUR),

    VICENTE_LOPEZ("Vicente López", Region.GBA_NORTE),
    SAN_ISIDRO("San Isidro", Region.GBA_NORTE),
    TIGRE("Tigre", Region.GBA_NORTE),

    LOMAS_DE_ZAMORA("Lomas de Zamora", Region.GBA_SUR),
    AVELLANEDA("Avellaneda", Region.GBA_SUR),
    QUILMES("Quilmes", Region.GBA_SUR);

    /**
     * Agrupación geográfica de zonas. Dos zonas de la misma región se consideran
     * "cercanas" entre sí.
     */
    public enum Region {
        CABA_NORTE, CABA_CENTRO, CABA_SUR, GBA_NORTE, GBA_SUR
    }

    /**
     * Nombre visible al usuario. Acá sí va como String literal y no como recurso:
     * son nombres propios de barrios, no se traducen.
     */
    private final String nombre;
    private final Region region;

    Zona(String nombre, Region region) {
        this.nombre = nombre;
        this.region = region;
    }

    public String getNombre() {
        return nombre;
    }

    public Region getRegion() {
        return region;
    }

    /** true si esta zona está en la misma región que {@code otra} (incluye ser la misma zona). */
    public boolean esCercanaA(Zona otra) {
        return otra != null && this.region == otra.region;
    }
}
