package com.example.tpo.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * La regla de "a dónde lleva tocar un aviso" (Punto 11). Los tipos son los que manda el backend.
 */
public class AvisoDestinoTest {

    @Test
    public void unaPreguntaNuevaAbreLaPublicacion() {
        AvisoDestino destino = AvisoDestino.desde("PREGUNTA_NUEVA", "12", null, "VENDEDOR");

        assertNotNull(destino);
        assertEquals(AvisoDestino.Tipo.PUBLICACION, destino.getTipo());
        assertEquals("12", destino.getPublicacionId());
        assertNull(destino.getOfertaId());
    }

    @Test
    public void unaRespuestaAbreLaPublicacion() {
        AvisoDestino destino = AvisoDestino.desde("PREGUNTA_RESPONDIDA", "12", null, "COMPRADOR");

        assertNotNull(destino);
        assertEquals(AvisoDestino.Tipo.PUBLICACION, destino.getTipo());
        assertEquals("12", destino.getPublicacionId());
    }

    @Test
    public void unaOfertaRecibidaAbreLaOfertaComoVendedor() {
        AvisoDestino destino = AvisoDestino.desde("OFERTA_RECIBIDA", "12", "7", "VENDEDOR");

        assertNotNull(destino);
        assertEquals(AvisoDestino.Tipo.OFERTA, destino.getTipo());
        assertEquals("7", destino.getOfertaId());
        assertEquals("12", destino.getPublicacionId());
        assertTrue(destino.esComoVendedor());
    }

    @Test
    public void unaOfertaAceptadaAbreLaOfertaComoComprador() {
        AvisoDestino destino = AvisoDestino.desde("OFERTA_ACEPTADA", "12", "7", "COMPRADOR");

        assertNotNull(destino);
        assertEquals(AvisoDestino.Tipo.OFERTA, destino.getTipo());
        assertFalse(destino.esComoVendedor());
    }

    @Test
    public void todosLosTiposDeOfertaAbrenLaOferta() {
        String[] tipos = {"OFERTA_RECIBIDA", "OFERTA_ACEPTADA", "OFERTA_RECHAZADA", "CONTRAOFERTA"};

        for (String tipo : tipos) {
            AvisoDestino destino = AvisoDestino.desde(tipo, "12", "7", "COMPRADOR");
            assertNotNull(tipo, destino);
            assertEquals(tipo, AvisoDestino.Tipo.OFERTA, destino.getTipo());
        }
    }

    @Test
    public void unaContraofertaRespetaElRolDeQuienLaRecibe() {
        AvisoDestino alVendedor = AvisoDestino.desde("CONTRAOFERTA", "12", "7", "VENDEDOR");
        AvisoDestino alComprador = AvisoDestino.desde("CONTRAOFERTA", "12", "7", "COMPRADOR");

        assertTrue(alVendedor.esComoVendedor());
        assertFalse(alComprador.esComoVendedor());
    }

    @Test
    public void sinRolOConUnRolRaroSeTomaComoComprador() {
        assertFalse(AvisoDestino.desde("OFERTA_RECIBIDA", "12", "7", null).esComoVendedor());
        assertFalse(AvisoDestino.desde("OFERTA_RECIBIDA", "12", "7", "").esComoVendedor());
        assertFalse(AvisoDestino.desde("OFERTA_RECIBIDA", "12", "7", "ADMIN").esComoVendedor());
    }

    @Test
    public void elRolNoDependeDeMayusculasNiEspacios() {
        assertTrue(AvisoDestino.desde("OFERTA_RECIBIDA", "12", "7", " vendedor ").esComoVendedor());
    }

    @Test
    public void unaOfertaSinIdDeOfertaCaeALaPublicacion() {
        AvisoDestino destino = AvisoDestino.desde("OFERTA_RECIBIDA", "12", null, "VENDEDOR");

        assertNotNull(destino);
        assertEquals(AvisoDestino.Tipo.PUBLICACION, destino.getTipo());
        assertEquals("12", destino.getPublicacionId());
    }

    @Test
    public void unaOfertaSinNingunIdNoTieneDestino() {
        assertNull(AvisoDestino.desde("OFERTA_RECIBIDA", null, null, "VENDEDOR"));
    }

    @Test
    public void unaPreguntaSinPublicacionNoTieneDestino() {
        assertNull(AvisoDestino.desde("PREGUNTA_NUEVA", null, null, "VENDEDOR"));
    }

    @Test
    public void unTipoDesconocidoConPublicacionAbreLaPublicacion() {
        AvisoDestino destino = AvisoDestino.desde("TIPO_NUEVO", "12", null, null);

        assertNotNull(destino);
        assertEquals(AvisoDestino.Tipo.PUBLICACION, destino.getTipo());
    }

    @Test
    public void unTipoDesconocidoConSoloOfertaNoTieneDestino() {
        // no se sabe qué es: sin publicación no hay a dónde ir
        assertNull(AvisoDestino.desde("TIPO_NUEVO", null, "7", null));
    }

    @Test
    public void sinTipoSeUsaLaPublicacion() {
        AvisoDestino destino = AvisoDestino.desde(null, "12", "7", "VENDEDOR");

        assertNotNull(destino);
        assertEquals(AvisoDestino.Tipo.PUBLICACION, destino.getTipo());
    }

    @Test
    public void losTextosVaciosSeTomanComoAusentes() {
        assertNull(AvisoDestino.desde("PREGUNTA_NUEVA", "", "", "VENDEDOR"));
        assertNull(AvisoDestino.desde("PREGUNTA_NUEVA", "   ", null, "VENDEDOR"));

        AvisoDestino destino = AvisoDestino.desde("OFERTA_RECIBIDA", "12", "  ", "VENDEDOR");
        assertEquals(AvisoDestino.Tipo.PUBLICACION, destino.getTipo());
    }

    @Test
    public void losIdsSeLimpianDeEspacios() {
        AvisoDestino destino = AvisoDestino.desde("OFERTA_RECIBIDA", " 12 ", " 7 ", "VENDEDOR");

        assertEquals("12", destino.getPublicacionId());
        assertEquals("7", destino.getOfertaId());
    }
}
