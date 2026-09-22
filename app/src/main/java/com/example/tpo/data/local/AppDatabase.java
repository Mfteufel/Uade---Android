package com.example.tpo.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Base de datos Room de Ronda.
 * <p>
 * Tiene la tabla del borrador de "Publicar artículo", la de "Mis
 * publicaciones" (Punto 5, reemplazo local mientras no exista el backend
 * real), el override de estado de la publicación del Detalle (Punto 4:
 * pausar/reactivar/vender), la de publicaciones vistas (Punto 6, modo sin
 * conexión) y la del catálogo publicado por el usuario (ver {@code version = 8}).
 * Preguntas y ofertas del Detalle ya no viven acá: se migraron al backend real
 * (ver {@code version = 9}). Se arma como singleton, igual que
 * {@code PublicacionRepositoryMock} y {@code SesionUsuario}, para no abrir más
 * de una conexión a la misma base.
 * <p>
 * {@code version = 5}: subió dos veces en paralelo (dos ramas distintas
 * llevaron {@code OfertaEntity} de v3 a v4, cada una con columnas propias) y
 * quedaron esquemas incompatibles bajo el mismo número. Sin ese bump, un
 * dispositivo que ya haya corrido una de las dos v4 crasheaba al abrir la app
 * con la otra ({@code IllegalStateException} de Room por hash de esquema
 * distinto) en vez de simplemente recrear la base, que es lo que hace
 * {@link androidx.room.RoomDatabase.Builder#fallbackToDestructiveMigration()}
 * — pero solo ante un cambio de versión, no si el número quedó pisado.
 * <p>
 * {@code version = 6}: se le agregó la columna {@code direccionEntrega} a
 * {@code BorradorPublicacionEntity} y a {@code PublicacionMiaEntity} (Punto 8:
 * el wizard de "Publicar artículo" ahora pide la dirección de entrega). Mismo
 * criterio que el bump anterior: sin subir la versión, Room ve un hash de
 * esquema distinto al de la base ya instalada y crashea en vez de recrearla.
 * <p>
 * {@code version = 7}: el Punto 10 (favoritos y búsquedas guardadas) sacó
 * {@code PublicacionGuardadaEntity} — el enunciado solo describe favoritos y
 * búsquedas guardadas, no un tercer concepto de "guardados" separado — así
 * que esa tabla se unificó dentro de favoritos y dejó de existir acá. Mismo
 * criterio de siempre: el esquema cambió (una tabla menos), así que necesita
 * su propio número de versión.
 * <p>
 * {@code version = 8}: se agrega {@code PublicacionCreadaEntity} (tabla
 * {@code publicacion_creada}). Antes de esto, la publicación que el usuario
 * creaba en el wizard se sumaba al catálogo mock de {@code PublicacionRepositoryMock}
 * solo en memoria: al reiniciar el proceso el catálogo se reconstruía desde
 * {@code crearCatalogoDePrueba()} (siempre las mismas 28 de prueba) y la publicación
 * nueva desaparecía del Home y de su propio Detalle, aunque seguía viéndose en "Mis
 * publicaciones" (Room). Esta tabla persiste esa publicación para que
 * {@code PublicacionRepositoryMock} la vuelva a sumar al catálogo en cada arranque.
 * <p>
 * {@code version = 9}: saca {@code PreguntaEntity} y {@code OfertaEntity}. El
 * Detalle (Punto 4) migró preguntas y ofertas al backend real
 * ({@code PreguntaRepositoryApi}, {@code OfertasRepository}) — las tablas
 * locales quedaron 100% muertas, junto con las clases que las usaban
 * ({@code PreguntasPublicacion}, {@code OfertasPublicacion}, el modelo
 * {@code Oferta}).
 * <p>
 * {@code version = 10}: {@code PublicacionVistaEntity} suma {@code fotos} y
 * {@code fotoPrincipalUrl}. Antes solo guardaba {@code cantidadFotos} (un
 * número), así que el modo sin conexión del Detalle (Punto 6) terminaba
 * mostrando placeholders en vez de las fotos reales que la persona ya había
 * visto online.
 */
@Database(entities = {
        BorradorPublicacionEntity.class,
        PublicacionMiaEntity.class,
        PublicacionEstadoEntity.class,
        PublicacionVistaEntity.class,
        PublicacionCreadaEntity.class
}, version = 10, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String NOMBRE_ARCHIVO = "ronda.db";

    private static volatile AppDatabase instancia;

    public abstract BorradorPublicacionDao borradorPublicacionDao();

    public abstract MiPublicacionDao miPublicacionDao();

    public abstract PublicacionEstadoDao publicacionEstadoDao();

    public abstract PublicacionVistaDao publicacionVistaDao();

    public abstract PublicacionCreadaDao publicacionCreadaDao();

    public static AppDatabase getInstancia(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    NOMBRE_ARCHIVO)
                            // No hay migraciones todavía (proyecto en desarrollo, sin
                            // datos de usuarios reales que preservar): un cambio de
                            // esquema simplemente recrea la base en vez de crashear.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instancia;
    }
}
