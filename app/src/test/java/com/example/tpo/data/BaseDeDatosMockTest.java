package com.example.tpo.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.tpo.model.Calificacion;
import com.example.tpo.model.EstadoOperacion;
import com.example.tpo.model.FiltroOperaciones;
import com.example.tpo.model.Operacion;
import com.example.tpo.model.Reputacion;
import com.example.tpo.model.TipoOperacion;
import com.example.tpo.model.Usuario;
import com.example.tpo.model.Zona;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Reglas de reputación y calificaciones (Puntos 2 y 9) sobre la base simulada.
 * <p>
 * Son las mismas reglas que tiene que cumplir el backend de Walter: sirven como
 * especificación ejecutable del contrato (ver docs/contrato-api-perfil-historial.md).
 * Usan un "ahora" fijo para que el plazo de 7 días no dependa del reloj.
 */
public class BaseDeDatosMockTest {

    private static final long DIA = BaseDeDatosMock.MILIS_POR_DIA;
    private static final long AHORA = 1_800_000_000_000L;

    private BaseDeDatosMock base;

    @Before
    public void prepararBase() {
        base = new BaseDeDatosMock();
        base.agregarUsuario("a", "Ana", "ana@ronda.com", "1111111111", Zona.CABALLITO, AHORA - 100 * DIA);
        base.agregarUsuario("b", "Beto", "beto@ronda.com", "2222222222", Zona.PALERMO, AHORA - 50 * DIA);
        base.agregarUsuario("c", "Caro", "caro@ronda.com", "", Zona.BOEDO, AHORA - 10 * DIA);
    }

    /** Ana le compra a Beto; la entrega fue hace {@code diasDesdeEntrega} días. */
    private String compraDeAnaABetoEntregadaHace(int diasDesdeEntrega) {
        String id = base.crearOperacion("p1", "Bicicleta", "a", "b", 85000, AHORA - (diasDesdeEntrega + 2) * DIA);
        base.registrarEntrega(id, AHORA - diasDesdeEntrega * DIA);
        return id;
    }

    // ------------------------------------------------------------------
    // Reputación
    // ------------------------------------------------------------------

    @Test
    public void reputacionSaleDeOperacionesEntregadasYCalificacionesRecibidas() {
        String compra = compraDeAnaABetoEntregadaHace(1);
        String venta = base.crearOperacion(null, "Libro", "c", "a", 1000, AHORA - 3 * DIA);
        base.registrarEntrega(venta, AHORA - 2 * DIA);
        // Pendiente de entrega: no cuenta como concretada.
        base.crearOperacion(null, "Lámpara", "a", "c", 500, AHORA);

        base.calificar(compra, "b", 4, null, AHORA);
        base.calificar(venta, "c", 5, "Genial", AHORA);

        Reputacion reputacion = base.calcularReputacion("a");
        assertEquals(4.5, reputacion.getPromedioEstrellas(), 0.001);
        assertEquals(2, reputacion.getCantidadCalificaciones());
        assertEquals(1, reputacion.getOperacionesComoComprador());
        assertEquals(1, reputacion.getOperacionesComoVendedor());
    }

    @Test
    public void conOperacionesPeroSinCalificacionesNoTieneCalificaciones() {
        compraDeAnaABetoEntregadaHace(1);

        Reputacion reputacion = base.calcularReputacion("a");
        assertEquals(1, reputacion.getOperacionesComoComprador());
        assertFalse(reputacion.tieneCalificaciones());
    }

    @Test
    public void calificarActualizaLaReputacionDelCalificadoYNoLaDelAutor() {
        String compra = compraDeAnaABetoEntregadaHace(1);

        base.calificar(compra, "a", 3, null, AHORA);

        assertEquals(3.0, base.calcularReputacion("b").getPromedioEstrellas(), 0.001);
        assertEquals(1, base.calcularReputacion("b").getCantidadCalificaciones());
        assertEquals(0, base.calcularReputacion("a").getCantidadCalificaciones());
    }

    @Test
    public void elCalificadoEsSiempreLaOtraParte() {
        String compra = compraDeAnaABetoEntregadaHace(1);

        base.calificar(compra, "a", 5, null, AHORA);

        List<Calificacion> deBeto = base.calificacionesRecibidas("b");
        assertEquals(1, deBeto.size());
        assertEquals("a", deBeto.get(0).getAutor().getId());
        assertTrue(base.calificacionesRecibidas("a").isEmpty());
    }

    // ------------------------------------------------------------------
    // Reglas para calificar
    // ------------------------------------------------------------------

    @Test
    public void sePuedeCalificarJustoEnElUltimoMomentoDelPlazo() {
        String compra = base.crearOperacion(null, "Bici", "a", "b", 100, AHORA - 10 * DIA);
        base.registrarEntrega(compra, AHORA - 7 * DIA);

        assertNull(base.validarCalificacion(compra, "a", 5, null, AHORA));
    }

    @Test
    public void noSePuedeCalificarPasadosLosSieteDias() {
        String compra = base.crearOperacion(null, "Bici", "a", "b", 100, AHORA - 10 * DIA);
        base.registrarEntrega(compra, AHORA - 7 * DIA - 1);

        assertNotNull(base.validarCalificacion(compra, "a", 5, null, AHORA));
    }

    @Test
    public void noSePuedeCalificarSinEntrega() {
        String pendiente = base.crearOperacion(null, "Bici", "a", "b", 100, AHORA - DIA);

        assertNotNull(base.validarCalificacion(pendiente, "a", 5, null, AHORA));
    }

    @Test
    public void noSePuedeCalificarDosVecesLaMismaOperacion() {
        String compra = compraDeAnaABetoEntregadaHace(1);
        base.calificar(compra, "a", 5, null, AHORA);

        assertNotNull(base.validarCalificacion(compra, "a", 4, null, AHORA));
        // La otra parte sí puede calificar la misma operación.
        assertNull(base.validarCalificacion(compra, "b", 4, null, AHORA));
    }

    @Test
    public void noSePuedeCalificarUnaOperacionAjena() {
        String compra = compraDeAnaABetoEntregadaHace(1);

        assertNotNull(base.validarCalificacion(compra, "c", 5, null, AHORA));
    }

    @Test
    public void lasEstrellasVanDeUnoACinco() {
        String compra = compraDeAnaABetoEntregadaHace(1);

        assertNotNull(base.validarCalificacion(compra, "a", 0, null, AHORA));
        assertNotNull(base.validarCalificacion(compra, "a", 6, null, AHORA));
        assertNull(base.validarCalificacion(compra, "a", 1, null, AHORA));
        assertNull(base.validarCalificacion(compra, "a", 5, null, AHORA));
    }

    @Test
    public void elComentarioEsOpcionalYTieneTope() {
        String compra = compraDeAnaABetoEntregadaHace(1);
        StringBuilder largo = new StringBuilder();
        for (int i = 0; i <= Calificacion.LARGO_MAXIMO_COMENTARIO; i++) {
            largo.append('x');
        }

        assertNull(base.validarCalificacion(compra, "a", 5, null, AHORA));
        assertNotNull(base.validarCalificacion(compra, "a", 5, largo.toString(), AHORA));
    }

    @Test
    public void unComentarioEnBlancoSeGuardaComoNulo() {
        String compra = compraDeAnaABetoEntregadaHace(1);

        base.calificar(compra, "a", 5, "   ", AHORA);

        assertNull(base.calificacionesRecibidas("b").get(0).getComentario());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nadiePuedeComprarseASiMismo() {
        base.crearOperacion(null, "Bici", "a", "a", 100, AHORA);
    }

    // ------------------------------------------------------------------
    // Entrega
    // ------------------------------------------------------------------

    @Test
    public void soloElCompradorConfirmaLaEntregaYUnaSolaVez() {
        String id = base.crearOperacion("p1", "Bicicleta", "a", "b", 85000, AHORA - DIA);

        assertEquals("Solo el comprador puede confirmar la entrega", base.validarEntrega(id, "b"));
        assertEquals("No participaste de esta operación", base.validarEntrega(id, "c"));
        assertNull(base.validarEntrega(id, "a"));

        Operacion entregada = base.confirmarEntrega(id, "a", AHORA);
        assertEquals(EstadoOperacion.ENTREGADA, entregada.getEstado());
        assertTrue(entregada.puedeCalificar());
        assertEquals("La entrega ya estaba confirmada", base.validarEntrega(id, "a"));
    }

    @Test
    public void unaVentaSinEntregaVaAPendientesYNoSumaHastaQueElCompradorConfirma() {
        String id = base.crearOperacion("p1", "Bicicleta", "a", "b", 85000, AHORA - 30 * DIA);

        // Aceptada pero sin entrega: no es historial ni suma a la reputación.
        assertTrue(base.historial("a", new FiltroOperaciones(), AHORA).isEmpty());
        assertEquals(TipoOperacion.COMPRA, base.pendientesDeEntrega("a", AHORA).get(0).getTipo());
        assertEquals(TipoOperacion.VENTA, base.pendientesDeEntrega("b", AHORA).get(0).getTipo());
        assertTrue(base.pendientesDeEntrega("c", AHORA).isEmpty());
        assertEquals(0, base.calcularReputacion("a").getOperacionesComoComprador());
        assertEquals(0, base.calcularReputacion("b").getOperacionesComoVendedor());

        Operacion entregada = base.confirmarEntrega(id, "a", AHORA);

        assertEquals(id, base.historial("a", new FiltroOperaciones(), AHORA).get(0).getId());
        assertTrue(base.pendientesDeEntrega("a", AHORA).isEmpty());
        assertEquals(1, base.calcularReputacion("a").getOperacionesComoComprador());
        assertEquals(1, base.calcularReputacion("b").getOperacionesComoVendedor());
        // Los 7 días corren desde la entrega, no desde el acuerdo de hace un mes.
        assertEquals(AHORA + 7 * DIA, (long) entregada.getCalificableHasta());
        assertTrue(entregada.puedeCalificar());
    }

    // ------------------------------------------------------------------
    // Historial
    // ------------------------------------------------------------------

    @Test
    public void elHistorialMuestraSoloOperacionesEntregadasPropiasVistasDesdeElUsuario() {
        String compra = compraDeAnaABetoEntregadaHace(1);
        base.crearOperacion(null, "Pendiente", "a", "b", 100, AHORA);
        String ajena = base.crearOperacion(null, "Ajena", "b", "c", 100, AHORA - 2 * DIA);
        base.registrarEntrega(ajena, AHORA - DIA);

        List<Operacion> deAna = base.historial("a", new FiltroOperaciones(), AHORA);
        assertEquals(1, deAna.size());
        assertEquals(compra, deAna.get(0).getId());
        assertEquals(TipoOperacion.COMPRA, deAna.get(0).getTipo());
        assertEquals("b", deAna.get(0).getContraparte().getId());

        // La misma operación, vista por Beto, es una venta con Ana de contraparte.
        Operacion vistaPorBeto = null;
        for (Operacion operacion : base.historial("b", new FiltroOperaciones(), AHORA)) {
            if (operacion.getId().equals(compra)) {
                vistaPorBeto = operacion;
            }
        }
        assertNotNull(vistaPorBeto);
        assertEquals(TipoOperacion.VENTA, vistaPorBeto.getTipo());
        assertEquals("a", vistaPorBeto.getContraparte().getId());
    }

    @Test
    public void elHistorialFiltraPorTipoYPorRangoDeFechasOrdenadoPorRecientes() {
        String vieja = compraDeAnaABetoEntregadaHace(30);
        String reciente = compraDeAnaABetoEntregadaHace(2);
        String venta = base.crearOperacion(null, "Libro", "c", "a", 1000, AHORA - 6 * DIA);
        base.registrarEntrega(venta, AHORA - 5 * DIA);

        FiltroOperaciones soloCompras = new FiltroOperaciones();
        soloCompras.setTipo(TipoOperacion.COMPRA);
        List<Operacion> compras = base.historial("a", soloCompras, AHORA);
        assertEquals(2, compras.size());
        assertEquals(reciente, compras.get(0).getId());
        assertEquals(vieja, compras.get(1).getId());

        FiltroOperaciones ultimaSemana = new FiltroOperaciones();
        ultimaSemana.setRangoFechas(AHORA - 7 * DIA, AHORA);
        List<Operacion> recientes = base.historial("a", ultimaSemana, AHORA);
        assertEquals(2, recientes.size());
        assertEquals(reciente, recientes.get(0).getId());
        assertEquals(venta, recientes.get(1).getId());
    }

    @Test
    public void cadaOperacionDiceSiSePuedeCalificarYHastaCuando() {
        String compra = compraDeAnaABetoEntregadaHace(2);

        Operacion antes = base.historial("a", new FiltroOperaciones(), AHORA).get(0);
        assertTrue(antes.puedeCalificar());
        assertFalse(antes.yaCalifique());
        assertEquals(AHORA - 2 * DIA + 7 * DIA, (long) antes.getCalificableHasta());

        Operacion despues = base.calificar(compra, "a", 4, "Todo bien", AHORA);
        assertFalse(despues.puedeCalificar());
        assertTrue(despues.yaCalifique());
        assertEquals(4, despues.getMiCalificacion().getEstrellas());
        assertEquals(0, base.contarCalificacionesPendientes("a", AHORA));
        assertEquals(1, base.contarCalificacionesPendientes("b", AHORA));
    }

    // ------------------------------------------------------------------
    // Perfil
    // ------------------------------------------------------------------

    @Test
    public void elPerfilPublicoNoExponeEmailNiTelefono() {
        Usuario publico = base.perfilPublico("a");
        assertNull(publico.getEmail());
        assertNull(publico.getTelefono());

        Usuario propio = base.perfilPropio("a", AHORA);
        assertEquals("ana@ronda.com", propio.getEmail());
    }

    @Test
    public void elEmailNoPuedeRepetirseEntreCuentas() {
        assertTrue(base.emailEnUso("BETO@ronda.com", "a"));
        assertFalse(base.emailEnUso("ana@ronda.com", "a"));
    }

    @Test
    public void losDatosDePruebaCubrenLosCasosDeLaDemo() {
        BaseDeDatosMock demo = BaseDeDatosMock.conDatosDePrueba(AHORA);
        String yo = BaseDeDatosMock.ID_USUARIO_DEMO;

        Reputacion reputacion = demo.calcularReputacion(yo);
        assertEquals(4.2, reputacion.getPromedioEstrellas(), 0.001);
        assertEquals(5, reputacion.getCantidadCalificaciones());
        assertEquals(5, reputacion.getOperacionesComoComprador());
        assertEquals(3, reputacion.getOperacionesComoVendedor());
        // Teclado, campera y bicicleta todavía se pueden calificar.
        assertEquals(3, demo.contarCalificacionesPendientes(yo, AHORA));
        // Los vendedores del catálogo del Detalle existen con los mismos ids.
        for (int i = 1; i <= 12; i++) {
            assertTrue(demo.existeUsuario("v" + i));
        }
    }
}
