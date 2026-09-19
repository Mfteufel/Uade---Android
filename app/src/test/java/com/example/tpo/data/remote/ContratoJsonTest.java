package com.example.tpo.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.tpo.data.remote.dto.UsuarioResponse;
import com.example.tpo.model.Usuario;
import com.example.tpo.model.Zona;
import com.google.gson.Gson;

import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Fija cómo lee la app los JSON de {@code docs/contrato-api-perfil-historial.md}.
 * <p>
 * Si el backend cambia un nombre de campo, este test es el que avisa qué DTO hay
 * que ajustar, sin tener que probarlo a mano en el emulador. Usa el mismo Gson
 * que Retrofit ({@code GsonConverterFactory.create()} sin configuración extra).
 */
public class ContratoJsonTest {

    private final Gson gson = new Gson();

    @Test
    public void perfilPropioConIdNumerico() {
        String json = "{\"id\": 1, \"nombre\": \"Walter\", \"email\": \"walter@uade.edu.ar\","
                + " \"telefono\": \"1145678901\", \"zona\": \"CABALLITO\","
                + " \"fecha_alta\": 1726000000000, \"foto_url\": \"usuarios/1/foto\","
                + " \"reputacion\": {\"promedio\": 4.2, \"cantidad_calificaciones\": 5,"
                + " \"operaciones_como_comprador\": 5, \"operaciones_como_vendedor\": 3},"
                + " \"calificaciones_pendientes\": 3}";

        Usuario usuario = gson.fromJson(json, UsuarioResponse.class).aModelo();

        assertEquals("1", usuario.getId());
        assertEquals(Zona.CABALLITO, usuario.getZona());
        assertEquals(1726000000000L, usuario.getFechaAlta());
        assertTrue(usuario.tieneFoto());
        assertEquals(4.2, usuario.getReputacion().getPromedioEstrellas(), 0.001);
        assertEquals(3, usuario.getReputacion().getOperacionesComoVendedor());
        assertEquals(3, usuario.getCalificacionesPendientes());
    }

    @Test
    public void perfilPublicoSinDatosPrivadosNiZonaConocida() {
        String json = "{\"id\": 7, \"nombre\": \"Martina G.\", \"zona\": \"ZONA_NUEVA\","
                + " \"fecha_alta\": 1700000000000, \"foto_url\": null,"
                + " \"reputacion\": {\"promedio\": 0, \"cantidad_calificaciones\": 0,"
                + " \"operaciones_como_comprador\": 0, \"operaciones_como_vendedor\": 0}}";

        Usuario usuario = gson.fromJson(json, UsuarioResponse.class).aModelo();

        assertNull(usuario.getEmail());
        assertNull(usuario.getTelefono());
        assertNull(usuario.getZona());
        assertFalse(usuario.tieneFoto());
        assertFalse(usuario.getReputacion().tieneCalificaciones());
        assertEquals(0, usuario.getCalificacionesPendientes());
    }

    @Test
    public void elMensajeDeErrorEsElDetailDelServidor() {
        Response<Object> conflicto = Response.error(409, cuerpo("{\"detail\": \"Ya calificaste esta operación\"}"));
        assertEquals("Ya calificaste esta operación", ErrorApi.mensaje(conflicto, "por defecto"));
    }

    @Test
    public void unErrorDeValidacionUsaElMensajePorDefecto() {
        Response<Object> invalido = Response.error(422,
                cuerpo("{\"detail\": [{\"loc\": [\"body\", \"estrellas\"], \"msg\": \"...\"}]}"));
        assertEquals("por defecto", ErrorApi.mensaje(invalido, "por defecto"));
    }

    @Test
    public void unCuatroCientosUnoPideVolverAIniciarSesion() {
        Response<Object> sinSesion = Response.error(401, cuerpo("{\"detail\": \"Necesitas iniciar sesion\"}"));
        assertTrue(ErrorApi.mensaje(sinSesion, "por defecto").contains("sesión"));
    }

    private static ResponseBody cuerpo(String json) {
        return ResponseBody.create(json, MediaType.get("application/json"));
    }
}
