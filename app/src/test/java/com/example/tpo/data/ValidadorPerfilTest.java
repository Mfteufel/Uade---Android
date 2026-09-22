package com.example.tpo.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.tpo.model.Reputacion;
import com.example.tpo.model.Usuario;
import com.example.tpo.model.Zona;

import org.junit.Test;

/**
 * Las validaciones que la app hace antes del {@code PUT /usuarios/yo}: el backend
 * todavía no valida formato, así que son las que evitan guardar datos absurdos.
 */
public class ValidadorPerfilTest {

    @Test
    public void datosCorrectosPasan() {
        assertNull(ValidadorPerfil.validar(con("Ana", "ana@ronda.com", "1155550000", Zona.PALERMO)));
        assertNull(ValidadorPerfil.validar(con("Ana", "ana@ronda.com", "+54 11 5555-0000", Zona.PALERMO)));
    }

    @Test
    public void elTelefonoEsOpcional() {
        assertNull(ValidadorPerfil.validar(con("Ana", "ana@ronda.com", "", Zona.PALERMO)));
        assertNull(ValidadorPerfil.validar(con("Ana", "ana@ronda.com", null, Zona.PALERMO)));
    }

    @Test
    public void nombreVacioOSoloEspaciosNoPasa() {
        assertNotNull(ValidadorPerfil.validar(con("", "ana@ronda.com", "", Zona.PALERMO)));
        assertNotNull(ValidadorPerfil.validar(con("   ", "ana@ronda.com", "", Zona.PALERMO)));
    }

    @Test
    public void nombreDemasiadoLargoNoPasa() {
        String largo = new String(new char[ValidadorPerfil.LARGO_MAXIMO_NOMBRE + 1]).replace('\0', 'a');
        assertNotNull(ValidadorPerfil.validar(con(largo, "ana@ronda.com", "", Zona.PALERMO)));
    }

    @Test
    public void emailInvalidoNoPasa() {
        assertNotNull(ValidadorPerfil.validar(con("Ana", "", "", Zona.PALERMO)));
        assertNotNull(ValidadorPerfil.validar(con("Ana", "ana", "", Zona.PALERMO)));
        assertNotNull(ValidadorPerfil.validar(con("Ana", "ana@ronda", "", Zona.PALERMO)));
        assertNotNull(ValidadorPerfil.validar(con("Ana", "ana @ronda.com", "", Zona.PALERMO)));
    }

    @Test
    public void telefonoInvalidoNoPasa() {
        assertNotNull(ValidadorPerfil.validar(con("Ana", "ana@ronda.com", "1234", Zona.PALERMO)));
        assertNotNull(ValidadorPerfil.validar(con("Ana", "ana@ronda.com", "11abc55550000", Zona.PALERMO)));
        assertNotNull(ValidadorPerfil.validar(con("Ana", "ana@ronda.com", "1234567890123456", Zona.PALERMO)));
    }

    @Test
    public void sinZonaNoPasa() {
        assertNotNull(ValidadorPerfil.validar(con("Ana", "ana@ronda.com", "", null)));
    }

    @Test
    public void normalizarRecortaYDejaElTelefonoVacioEnNull() {
        Usuario normalizado = ValidadorPerfil.normalizar(
                con("  Ana ", " Ana@Ronda.com ", "   ", Zona.PALERMO));

        assertEquals("Ana", normalizado.getNombre());
        assertEquals("ana@ronda.com", normalizado.getEmail());
        assertNull(normalizado.getTelefono());
        assertEquals(Zona.PALERMO, normalizado.getZona());
        // Lo que no se edita se conserva.
        assertEquals("2", normalizado.getId());
        assertEquals(1789862004400L, normalizado.getFechaAlta());
    }

    private static Usuario con(String nombre, String email, String telefono, Zona zona) {
        return new Usuario("2", nombre, email, telefono, zona, 1789862004400L,
                new Reputacion(0, 0, 0, 0), null, 0);
    }
}
