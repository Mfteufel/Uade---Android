package com.example.tpo.model;

/**
 * Modelo de datos para las novedades/avisos recibidos por long polling (Punto 11).
 * <p>
 * Representa una notificación emitida por el servidor ante eventos como nuevas
 * ofertas o preguntas. Se parsea directamente desde JSON mediante Gson.
 */
public class Novedad {

    private String id;
    private String tipo;
    private String titulo;
    private String cuerpo;
    private String publicacionId;
    private String ofertaId;
    private String rol;
    private long creadoEn;

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public String getPublicacionId() {
        return publicacionId;
    }

    public String getOfertaId() {
        return ofertaId;
    }

    public String getRol() {
        return rol;
    }

    public long getCreadoEn() {
        return creadoEn;
    }
}
