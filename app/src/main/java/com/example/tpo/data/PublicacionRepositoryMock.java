package com.example.tpo.data;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.tpo.model.Categoria;
import com.example.tpo.model.Cercania;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.FiltroPublicaciones;
import com.example.tpo.model.Publicacion;
import com.example.tpo.model.Zona;
import com.example.tpo.util.TextoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implementación de {@link PublicacionRepository} con datos fijos en memoria.
 * <p>
 * Es la simplificación acordada para las primeras entregas: la API_Rest del TPO
 * todavía no existe, así que el listado se resuelve sobre una lista armada acá.
 * <p>
 * <b>Cómo se reemplaza por la API de verdad:</b> se crea un
 * {@code PublicacionRepositoryApi} que implemente la misma interfaz y que adentro
 * de {@link #buscarPublicaciones} haga {@code call.enqueue(...)} con Retrofit,
 * validando {@code response.isSuccessful()} antes de leer el body. El Home no se
 * modifica porque depende de la interfaz, no de esta clase.
 * <p>
 * Ojo con un detalle importante: el filtrado, el ordenamiento y el recorte de la
 * página se hacen acá y no en el Fragment. Esa es la responsabilidad que en la app
 * final tendrá el backend, así que dejarla del lado del repositorio es lo que hace
 * que el reemplazo sea directo.
 */
public class PublicacionRepositoryMock implements PublicacionRepository {

    /**
     * Demora artificial de la respuesta, en milisegundos.
     * <p>
     * Sin esto los datos aparecerían instantáneamente y nunca veríamos el estado
     * de carga; con la demora la pantalla se comporta como se va a comportar
     * contra el backend real.
     */
    private static final long DEMORA_SIMULADA_MS = 600;

    /**
     * Poner en true para probar cómo se ve la pantalla de error sin necesidad de
     * tener el backend caído. Debe quedar en false en lo que se entrega.
     */
    private static final boolean SIMULAR_ERROR = false;

    private static PublicacionRepositoryMock instancia;

    /** Catálogo completo. Se arma una sola vez para que las fechas no cambien entre consultas. */
    private final List<Publicacion> catalogo;

    /** Handler del Main Thread: garantiza que el callback llegue donde se puede tocar la UI. */
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    private PublicacionRepositoryMock() {
        catalogo = crearCatalogoDePrueba();
    }

    public static synchronized PublicacionRepositoryMock getInstancia() {
        if (instancia == null) {
            instancia = new PublicacionRepositoryMock();
        }
        return instancia;
    }

    @Override
    public void buscarPublicaciones(FiltroPublicaciones filtro,
                                    int pagina,
                                    RepositorioCallback<PaginaPublicaciones> callback) {
        // postDelayed simula la latencia de red. El callback termina ejecutándose
        // en el Main Thread, igual que onResponse() de Retrofit.
        handlerPrincipal.postDelayed(() -> {
            if (SIMULAR_ERROR) {
                callback.onError("No pudimos cargar las publicaciones");
                return;
            }

            List<Publicacion> resultado = aplicarFiltros(filtro);
            ordenar(resultado, filtro);
            callback.onExito(recortarPagina(resultado, pagina));
        }, DEMORA_SIMULADA_MS);
    }

    // ---------------------------------------------------------------------
    // Filtrado
    // ---------------------------------------------------------------------

    /** Aplica todos los criterios del filtro. Un artículo tiene que pasar todos para entrar. */
    private List<Publicacion> aplicarFiltros(FiltroPublicaciones filtro) {
        List<Publicacion> resultado = new ArrayList<>();
        for (Publicacion publicacion : catalogo) {
            if (coincideConFiltro(publicacion, filtro)) {
                resultado.add(publicacion);
            }
        }
        return resultado;
    }

    /**
     * true si la publicación pasa todos los criterios del filtro. Se expone
     * público porque {@code BusquedaGuardadaRepositoryMock} también la usa,
     * para saber si una publicación nueva matchea una búsqueda guardada
     * (Punto 10, indicador de novedad).
     */
    public boolean coincideConFiltro(Publicacion publicacion, FiltroPublicaciones filtro) {
        String textoBuscado = TextoUtils.normalizar(filtro.getTexto());
        Zona zonaUsuario = SesionUsuario.getInstancia().getZona();

        if (!coincideTexto(publicacion, textoBuscado)) {
            return false;
        }
        if (filtro.getCategoria() != null && publicacion.getCategoria() != filtro.getCategoria()) {
            return false;
        }
        // Set vacío = el usuario no filtró por estado, entran todos.
        if (!filtro.getEstados().isEmpty() && !filtro.getEstados().contains(publicacion.getEstado())) {
            return false;
        }
        if (!coincidePrecio(publicacion, filtro)) {
            return false;
        }
        return coincideCercania(publicacion, filtro.getCercania(), zonaUsuario);
    }

    /** Búsqueda por texto libre sobre título y descripción, ignorando mayúsculas y tildes. */
    private boolean coincideTexto(Publicacion publicacion, String textoNormalizado) {
        if (textoNormalizado.isEmpty()) {
            return true;
        }
        return TextoUtils.normalizar(publicacion.getTitulo()).contains(textoNormalizado)
                || TextoUtils.normalizar(publicacion.getDescripcion()).contains(textoNormalizado);
    }

    /** Rango de precio. Cada extremo es opcional por separado. */
    private boolean coincidePrecio(Publicacion publicacion, FiltroPublicaciones filtro) {
        Double minimo = filtro.getPrecioMinimo();
        Double maximo = filtro.getPrecioMaximo();
        if (minimo != null && publicacion.getPrecio() < minimo) {
            return false;
        }
        return maximo == null || publicacion.getPrecio() <= maximo;
    }

    /** Cercanía a la zona del usuario: misma zona exacta, misma región, o sin filtrar. */
    private boolean coincideCercania(Publicacion publicacion, Cercania cercania, Zona zonaUsuario) {
        if (cercania == Cercania.TODAS || zonaUsuario == null) {
            return true;
        }
        if (cercania == Cercania.MI_ZONA) {
            return publicacion.getZona() == zonaUsuario;
        }
        return publicacion.getZona().esCercanaA(zonaUsuario);
    }

    // ---------------------------------------------------------------------
    // Ordenamiento y paginado
    // ---------------------------------------------------------------------

    private void ordenar(List<Publicacion> publicaciones, FiltroPublicaciones filtro) {
        Comparator<Publicacion> comparador;
        switch (filtro.getOrden()) {
            case PRECIO_MENOR:
                comparador = (a, b) -> Double.compare(a.getPrecio(), b.getPrecio());
                break;
            case PRECIO_MAYOR:
                comparador = (a, b) -> Double.compare(b.getPrecio(), a.getPrecio());
                break;
            case RECIENTES:
            default:
                // Fecha descendente: la publicación más nueva primero.
                comparador = (a, b) -> Long.compare(b.getFechaPublicacion(), a.getFechaPublicacion());
                break;
        }
        Collections.sort(publicaciones, comparador);
    }

    /**
     * Recorta la página pedida de la lista ya filtrada y ordenada.
     * <p>
     * Si la página pedida arranca más allá del final devuelve una página vacía en
     * vez de romper: es el caso normal cuando el usuario llega al final del scroll.
     */
    private PaginaPublicaciones recortarPagina(List<Publicacion> resultado, int pagina) {
        int total = resultado.size();
        int desde = pagina * TAMANIO_PAGINA;

        if (desde >= total) {
            return new PaginaPublicaciones(new ArrayList<>(), pagina, false, total);
        }

        int hasta = Math.min(desde + TAMANIO_PAGINA, total);
        // subList devuelve una vista sobre la lista original; se copia para que el
        // adapter no quede atado a ella.
        List<Publicacion> pagina0 = new ArrayList<>(resultado.subList(desde, hasta));
        boolean hayMas = hasta < total;
        return new PaginaPublicaciones(pagina0, pagina, hayMas, total);
    }

    /**
     * Agrega una publicación nueva al catálogo, para probar el indicador de
     * novedad de las búsquedas guardadas sin backend. Se dispara vía ADB, que le pasa título y
     * categoría opcionales para poder matchear la búsqueda guardada que se
     * esté probando (el resto de los criterios queda fijo).
     */
    public Publicacion agregarPublicacionDePrueba(@Nullable String titulo, @Nullable Categoria categoria) {
        Publicacion nueva = new Publicacion(
                "debug-" + System.currentTimeMillis(),
                titulo != null ? titulo : "Publicación nueva de prueba",
                "Generada a mano para probar el indicador de novedad del Punto 10.",
                50000, EstadoArticulo.NUEVO, categoria != null ? categoria : Categoria.OTROS, Zona.CABALLITO,
                System.currentTimeMillis(), "Cuenta de prueba");
        catalogo.add(nueva);
        return nueva;
    }

    // ---------------------------------------------------------------------
    // Datos de prueba
    // ---------------------------------------------------------------------

    /** Milisegundos correspondientes a "hace N días y M horas". */
    private static long hace(int dias, int horas) {
        long ahora = System.currentTimeMillis();
        return ahora - (dias * 24L + horas) * 60L * 60L * 1000L;
    }

    /**
     * Catálogo de prueba. Está armado a propósito con variedad de categorías,
     * estados, zonas, precios y fechas para que se note el efecto de cada filtro
     * y de cada criterio de ordenamiento.
     */
    private static List<Publicacion> crearCatalogoDePrueba() {
        List<Publicacion> lista = new ArrayList<>();

        lista.add(new Publicacion("1", "iPhone 13 128GB",
                "Batería al 89%, funda y cargador original incluidos. Sin detalles en pantalla.",
                620000, EstadoArticulo.COMO_NUEVO, Categoria.TECNOLOGIA, Zona.PALERMO,
                hace(0, 2), "Martina G."));
        lista.add(new Publicacion("2", "Notebook Lenovo IdeaPad 15",
                "i5 de 11va generación, 16GB de RAM y SSD de 512GB. Ideal para estudiar o trabajar.",
                480000, EstadoArticulo.USADO, Categoria.TECNOLOGIA, Zona.CABALLITO,
                hace(0, 5), "Nicolás P."));
        lista.add(new Publicacion("3", "Monitor Samsung 24\" curvo",
                "Full HD 75Hz. Lo uso poco desde que armé la PC nueva. Incluye cable HDMI.",
                165000, EstadoArticulo.USADO, Categoria.TECNOLOGIA, Zona.BELGRANO,
                hace(1, 3), "Luciano R."));
        lista.add(new Publicacion("4", "Teclado mecánico Redragon",
                "Switches red, retroiluminado RGB. Sin uso, me lo regalaron repetido.",
                52000, EstadoArticulo.NUEVO, Categoria.TECNOLOGIA, Zona.ALMAGRO,
                hace(2, 1), "Sofía M."));

        lista.add(new Publicacion("5", "Sillón de dos cuerpos",
                "Tapizado en pana gris. Muy cómodo, lo vendo por mudanza. Retira en el día.",
                210000, EstadoArticulo.USADO, Categoria.HOGAR, Zona.VILLA_CRESPO,
                hace(0, 8), "Familia Ruiz"));
        lista.add(new Publicacion("6", "Mesa de comedor extensible",
                "Madera de paraíso, para 6 u 8 personas. Tiene marcas de uso en la tapa.",
                175000, EstadoArticulo.USADO, Categoria.HOGAR, Zona.FLORES,
                hace(3, 6), "Carlos D."));
        lista.add(new Publicacion("7", "Cafetera express Philips",
                "La usé menos de diez veces. Está impecable, con manual y caja.",
                145000, EstadoArticulo.COMO_NUEVO, Categoria.HOGAR, Zona.SAN_ISIDRO,
                hace(1, 10), "Valeria S."));
        lista.add(new Publicacion("8", "Juego de sábanas queen",
                "Algodón 200 hilos, sin estrenar. Color blanco.",
                38000, EstadoArticulo.NUEVO, Categoria.HOGAR, Zona.QUILMES,
                hace(4, 2), "Ana L."));

        lista.add(new Publicacion("9", "Campera de cuero negra",
                "Talle M, cuero ecológico. Muy poco uso, quedó chica.",
                85000, EstadoArticulo.COMO_NUEVO, Categoria.INDUMENTARIA, Zona.RECOLETA,
                hace(0, 14), "Julieta F."));
        lista.add(new Publicacion("10", "Zapatillas Nike Air Max 90",
                "Talle 42, usadas un par de veces. Vienen con la caja original.",
                95000, EstadoArticulo.COMO_NUEVO, Categoria.INDUMENTARIA, Zona.NUNEZ,
                hace(2, 7), "Bruno T."));
        lista.add(new Publicacion("11", "Vestido de fiesta largo",
                "Talle S, azul noche. Usado una sola vez en un casamiento.",
                62000, EstadoArticulo.COMO_NUEVO, Categoria.INDUMENTARIA, Zona.VICENTE_LOPEZ,
                hace(5, 4), "Camila V."));

        lista.add(new Publicacion("12", "Bicicleta mountain bike rodado 29",
                "Cuadro de aluminio, 21 cambios Shimano. Recién service completo.",
                320000, EstadoArticulo.USADO, Categoria.DEPORTES, Zona.TIGRE,
                hace(1, 1), "Federico A."));
        lista.add(new Publicacion("13", "Set de mancuernas 20kg",
                "Discos de goma con barra ajustable. Las uso desde que armé el gimnasio en casa.",
                78000, EstadoArticulo.USADO, Categoria.DEPORTES, Zona.BOEDO,
                hace(3, 9), "Gonzalo H."));
        lista.add(new Publicacion("14", "Cinta de correr plegable",
                "Motor 2HP, se pliega para guardar. Funciona perfecto, la vendo por espacio.",
                410000, EstadoArticulo.USADO, Categoria.DEPORTES, Zona.LOMAS_DE_ZAMORA,
                hace(6, 3), "Mariano C."));
        lista.add(new Publicacion("15", "Pelota de fútbol profesional",
                "Nueva, todavía en la bolsa. Número 5, cosida a mano.",
                29000, EstadoArticulo.NUEVO, Categoria.DEPORTES, Zona.AVELLANEDA,
                hace(7, 5), "Club San Martín"));

        lista.add(new Publicacion("16", "Colección Harry Potter completa",
                "Los siete libros en tapa dura, edición Salamandra. Muy bien cuidados.",
                115000, EstadoArticulo.COMO_NUEVO, Categoria.LIBROS, Zona.CABALLITO,
                hace(0, 20), "Paula E."));
        lista.add(new Publicacion("17", "Rayuela - Julio Cortázar",
                "Edición de bolsillo, con algunas anotaciones al margen en lápiz.",
                12000, EstadoArticulo.USADO, Categoria.LIBROS, Zona.ALMAGRO,
                hace(2, 12), "Diego B."));
        lista.add(new Publicacion("18", "Manual de Anatomía de Rouvière",
                "Tomo 1 y 2. Los usé toda la carrera, están completos y sin hojas sueltas.",
                68000, EstadoArticulo.USADO, Categoria.LIBROS, Zona.BARRACAS,
                hace(8, 1), "Rocío N."));

        lista.add(new Publicacion("19", "Guitarra criolla Fonseca",
                "Modelo 40, con funda acolchada. Encordado nuevo puesto la semana pasada.",
                135000, EstadoArticulo.USADO, Categoria.INSTRUMENTOS, Zona.PALERMO,
                hace(1, 16), "Tomás I."));
        lista.add(new Publicacion("20", "Teclado Yamaha PSR-E373",
                "61 teclas sensibles, con fuente y atril. Comprado hace seis meses.",
                295000, EstadoArticulo.COMO_NUEVO, Categoria.INSTRUMENTOS, Zona.BELGRANO,
                hace(4, 8), "Escuela de Música"));
        lista.add(new Publicacion("21", "Amplificador Marshall 15W",
                "Ideal para practicar en casa. Tiene distorsión y reverb.",
                160000, EstadoArticulo.USADO, Categoria.INSTRUMENTOS, Zona.VILLA_CRESPO,
                hace(9, 2), "Ezequiel O."));

        lista.add(new Publicacion("22", "Cochecito Infanti 3 en 1",
                "Incluye huevito y base para auto. Usado por un solo bebé.",
                240000, EstadoArticulo.USADO, Categoria.BEBES, Zona.SAN_ISIDRO,
                hace(0, 11), "Laura y Seba"));
        lista.add(new Publicacion("23", "Cuna funcional de madera",
                "Se convierte en cama de una plaza. Colchón incluido, sin manchas.",
                185000, EstadoArticulo.COMO_NUEVO, Categoria.BEBES, Zona.VICENTE_LOPEZ,
                hace(5, 15), "Melina Q."));
        lista.add(new Publicacion("24", "Lote de ropa de bebé 0 a 6 meses",
                "Aproximadamente 30 prendas, todas lavadas y en buen estado.",
                35000, EstadoArticulo.USADO, Categoria.BEBES, Zona.QUILMES,
                hace(10, 4), "Andrea Z."));

        lista.add(new Publicacion("25", "Caja de herramientas completa",
                "Llaves, destornilladores, pinzas y taladro. Todo en su maletín.",
                125000, EstadoArticulo.USADO, Categoria.OTROS, Zona.BARRACAS,
                hace(6, 18), "Ricardo U."));
        lista.add(new Publicacion("26", "Rompecabezas 3000 piezas",
                "Armado una sola vez, están todas las piezas. Motivo: mapa antiguo.",
                18000, EstadoArticulo.COMO_NUEVO, Categoria.OTROS, Zona.FLORES,
                hace(11, 6), "Silvina K."));
        lista.add(new Publicacion("27", "Escritorio con estantería",
                "Melamina blanca, 120cm de ancho. Se desarma para el traslado.",
                98000, EstadoArticulo.USADO, Categoria.HOGAR, Zona.BOEDO,
                hace(12, 3), "Hernán W."));
        lista.add(new Publicacion("28", "Aire acondicionado split 3000 frigorías",
                "Frío/calor, funcionando perfecto. Se retira ya desinstalado.",
                390000, EstadoArticulo.USADO, Categoria.HOGAR, Zona.AVELLANEDA,
                hace(13, 9), "Gabriel Y."));

        return lista;
    }
}
