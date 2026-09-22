package com.example.tpo.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.tpo.data.remote.dto.ActualizarPerfilRequest;
import com.example.tpo.data.remote.dto.OperacionResponse;
import com.example.tpo.data.remote.dto.PaginaPublicacionesResponse;
import com.example.tpo.data.remote.dto.PublicacionResumenResponse;
import com.example.tpo.data.remote.dto.UsuarioResponse;
import com.example.tpo.model.Categoria;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.EstadoOperacion;
import com.example.tpo.model.EstadoPublicacion;
import com.example.tpo.model.Operacion;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Reputacion;
import com.example.tpo.model.TipoOperacion;
import com.example.tpo.model.Usuario;
import com.example.tpo.model.Zona;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

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

    /**
     * Misma forma que {@code GET /usuarios/yo} en Railway. La reputación va con
     * valores distintos de cero a propósito: hoy el backend la manda en cero, y un
     * cero también es lo que queda si Gson no encuentra el campo.
     */
    @Test
    public void perfilPropioComoLoDevuelveElBackend() {
        String json = "{\"id\": \"2\", \"nombre\": \"Ana\", \"email\": \"ana@ronda.com\","
                + " \"telefono\": null, \"zona\": \"PALERMO\", \"fechaAlta\": 1789862004400,"
                + " \"reputacion\": {\"promedioEstrellas\": 4.5,"
                + " \"operacionesComoComprador\": 2, \"operacionesComoVendedor\": 3}}";

        Usuario usuario = gson.fromJson(json, UsuarioResponse.class).aModelo();

        assertEquals("2", usuario.getId());
        assertEquals("Ana", usuario.getNombre());
        assertEquals("ana@ronda.com", usuario.getEmail());
        assertNull(usuario.getTelefono());
        assertEquals(Zona.PALERMO, usuario.getZona());
        assertEquals(1789862004400L, usuario.getFechaAlta());
        assertEquals(4.5, usuario.getReputacion().getPromedioEstrellas(), 0.001);
        assertEquals(2, usuario.getReputacion().getOperacionesComoComprador());
        assertEquals(3, usuario.getReputacion().getOperacionesComoVendedor());
        // El backend no manda foto ni calificaciones pendientes.
        assertFalse(usuario.tieneFoto());
        assertEquals(0, usuario.getCalificacionesPendientes());
    }

    @Test
    public void perfilPublicoSinDatosPrivadosNiZonaConocida() {
        String json = "{\"id\": \"7\", \"nombre\": \"Martina G.\", \"email\": null,"
                + " \"telefono\": null, \"zona\": \"ZONA_NUEVA\", \"fechaAlta\": 1789862004383,"
                + " \"reputacion\": {\"promedioEstrellas\": 0.0,"
                + " \"operacionesComoComprador\": 0, \"operacionesComoVendedor\": 0}}";

        Usuario usuario = gson.fromJson(json, UsuarioResponse.class).aModelo();

        assertNull(usuario.getEmail());
        assertNull(usuario.getTelefono());
        assertNull(usuario.getZona());
        assertEquals(1789862004383L, usuario.getFechaAlta());
        assertFalse(usuario.tieneFoto());
        assertFalse(usuario.getReputacion().tieneCalificaciones());
        assertEquals(0, usuario.getCalificacionesPendientes());
    }

    /** {@code GET /usuarios/2} en Railway: el perfil público manda email y teléfono en null. */
    @Test
    public void perfilPublicoComoLoDevuelveElBackend() {
        String json = "{\"id\":\"2\",\"nombre\":\"Ana\",\"email\":null,\"telefono\":null,"
                + "\"zona\":\"PALERMO\",\"fechaAlta\":1789862004400,"
                + "\"reputacion\":{\"promedioEstrellas\":0.0,"
                + "\"operacionesComoComprador\":0,\"operacionesComoVendedor\":0}}";

        Usuario usuario = gson.fromJson(json, UsuarioResponse.class).aModelo();

        assertEquals("2", usuario.getId());
        assertEquals("Ana", usuario.getNombre());
        assertNull(usuario.getEmail());
        assertNull(usuario.getTelefono());
        assertEquals(Zona.PALERMO, usuario.getZona());
        assertEquals(1789862004400L, usuario.getFechaAlta());
        assertFalse(usuario.getReputacion().tieneCalificaciones());
        assertEquals(0, usuario.getReputacion().getTotalOperaciones());
    }

    /** El PUT reemplaza el perfil entero: tienen que viajar los cuatro campos. */
    @Test
    public void laEdicionMandaLosCuatroCamposConLaZonaComoEnum() {
        Usuario editado = usuarioAna().conDatosPersonales("Ana", "ana@ronda.com",
                "1155550000", Zona.PALERMO);

        JsonObject cuerpo = gson.toJsonTree(new ActualizarPerfilRequest(editado)).getAsJsonObject();

        assertEquals(4, cuerpo.size());
        assertEquals("Ana", cuerpo.get("nombre").getAsString());
        assertEquals("ana@ronda.com", cuerpo.get("email").getAsString());
        assertEquals("1155550000", cuerpo.get("telefono").getAsString());
        assertEquals("PALERMO", cuerpo.get("zona").getAsString());
    }

    /**
     * Borrar el teléfono lo deja en null del otro lado: Gson omite el campo y el
     * backend guarda null, igual que un usuario que nunca lo cargó.
     */
    @Test
    public void unTelefonoVacioNoViajaComoTextoVacio() {
        Usuario editado = usuarioAna().conDatosPersonales("Ana", "ana@ronda.com", "", Zona.PALERMO);

        ActualizarPerfilRequest request = new ActualizarPerfilRequest(editado);
        JsonObject cuerpo = gson.toJsonTree(request).getAsJsonObject();

        assertNull(request.telefono);
        assertFalse(cuerpo.has("telefono"));
        assertEquals("PALERMO", cuerpo.get("zona").getAsString());
    }

    /** Forma de {@code GET /publicaciones?vendedorId=2} en Railway. */
    @Test
    public void publicacionesDeUnVendedor() {
        String json = "{\"publicaciones\":[{\"id\":\"1\",\"titulo\":\"Celular Motorola G54\","
                + "\"descripcion\":\"Con caja y cargador.\",\"precio\":280000.0,"
                + "\"categoria\":\"TECNOLOGIA\",\"estadoArticulo\":\"COMO_NUEVO\","
                + "\"zona\":\"PALERMO\",\"estadoPublicacion\":\"ACTIVA\","
                + "\"fechaPublicacion\":1789854804438,\"vendedorId\":\"2\","
                + "\"nombreVendedor\":\"Ana\",\"fotoPrincipalUrl\":null}],"
                + "\"pagina\":0,\"hayMas\":false,\"totalResultados\":1}";

        PaginaPublicacionesResponse pagina = gson.fromJson(json, PaginaPublicacionesResponse.class);
        Publicacion publicacion = pagina.publicaciones.get(0).aModelo();

        assertEquals(0, pagina.pagina);
        assertFalse(pagina.hayMas);
        assertEquals(1, pagina.totalResultados);
        assertNotNull(publicacion);
        assertEquals("1", publicacion.getId());
        assertEquals("Celular Motorola G54", publicacion.getTitulo());
        assertEquals(280000.0, publicacion.getPrecio(), 0.001);
        assertEquals(Categoria.TECNOLOGIA, publicacion.getCategoria());
        assertEquals(EstadoArticulo.COMO_NUEVO, publicacion.getEstado());
        assertEquals(Zona.PALERMO, publicacion.getZona());
        assertEquals(EstadoPublicacion.ACTIVA, publicacion.getEstadoPublicacion());
        assertEquals(1789854804438L, publicacion.getFechaPublicacion());
        assertEquals("2", publicacion.getVendedor().getId());
        assertEquals("Ana", publicacion.getVendedor().getNombre());
        assertEquals(0, publicacion.getCantidadFotos());
    }

    @Test
    public void unVendedorSinPublicacionesEsUnaListaVacia() {
        String json = "{\"publicaciones\":[],\"pagina\":0,\"hayMas\":false,\"totalResultados\":0}";

        PaginaPublicacionesResponse pagina = gson.fromJson(json, PaginaPublicacionesResponse.class);

        assertTrue(pagina.publicaciones.isEmpty());
        assertEquals(0, pagina.totalResultados);
    }

    /** La tarjeta del listado necesita estado y zona: un valor desconocido no se muestra. */
    @Test
    public void unaPublicacionConUnValorDesconocidoSeDescarta() {
        String json = "{\"id\":\"9\",\"titulo\":\"X\",\"precio\":1,\"categoria\":\"TECNOLOGIA\","
                + "\"estadoArticulo\":\"REACONDICIONADO\",\"zona\":\"PALERMO\","
                + "\"estadoPublicacion\":\"ACTIVA\",\"vendedorId\":\"2\"}";

        assertNull(gson.fromJson(json, PublicacionResumenResponse.class).aModelo());
    }

    @Test
    public void operacionVistaPorElComprador() {
        String json = "{\"id\": 12, \"publicacion_id\": 4, \"articulo\": \"Teclado mecánico\","
                + " \"monto_final\": 45000, \"fecha_operacion\": 1726000000000,"
                + " \"fecha_entrega\": 1726200000000, \"estado\": \"ENTREGADA\", \"tipo\": \"COMPRA\","
                + " \"comprador\": {\"id\": 1, \"nombre\": \"Walter\"},"
                + " \"vendedor\": {\"id\": 3, \"nombre\": \"Sofía M.\"},"
                + " \"mi_calificacion\": null, \"puede_calificar\": true,"
                + " \"calificable_hasta\": 1726804800000}";

        Operacion operacion = gson.fromJson(json, OperacionResponse.class).aModelo();

        assertEquals("12", operacion.getId());
        assertEquals(TipoOperacion.COMPRA, operacion.getTipo());
        assertEquals(EstadoOperacion.ENTREGADA, operacion.getEstado());
        assertEquals("Sofía M.", operacion.getContraparte().getNombre());
        assertEquals(45000, operacion.getMontoFinal(), 0.001);
        assertTrue(operacion.puedeCalificar());
        assertFalse(operacion.yaCalifique());
        assertEquals(1726804800000L, (long) operacion.getCalificableHasta());
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

    private static Usuario usuarioAna() {
        return new Usuario("2", "Ana", "ana@ronda.com", null, Zona.PALERMO, 1789862004400L,
                new Reputacion(0, 0, 0, 0), null, 0);
    }

    private static ResponseBody cuerpo(String json) {
        return ResponseBody.create(json, MediaType.get("application/json"));
    }
}
