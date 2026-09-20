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
 * real), las cuatro tablas de persistencia del Detalle (Punto 4): guardados,
 * preguntas, ofertas (con su ciclo de negociación completo del Punto 7) y el
 * override de estado de la publicación, y la de publicaciones vistas (Punto 6,
 * modo sin conexión). Se arma como singleton, igual que
 * {@code PublicacionRepositoryMock} y {@code SesionUsuario}, para no abrir
 * más de una conexión a la misma base.
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
 * {@code version = 7}: al traer main de nuevo, otra rama había sumado en
 * paralelo la tabla {@code publicaciones_vistas} (Punto 6) bajo su propio
 * {@code version = 5} — ya van dos esquemas distintos numerados 5 (ver el
 * párrafo anterior), más el 6 de {@code direccionEntrega}, todos incompatibles
 * entre sí bajo el mismo número. Este esquema mergeado (las 7 entidades) no
 * coincide con ninguno de los anteriores, así que necesita número propio.
 */
@Database(entities = {
        BorradorPublicacionEntity.class,
        PublicacionMiaEntity.class,
        PublicacionEstadoEntity.class,
        PublicacionGuardadaEntity.class,
        PreguntaEntity.class,
        OfertaEntity.class,
        PublicacionVistaEntity.class
}, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String NOMBRE_ARCHIVO = "ronda.db";

    private static volatile AppDatabase instancia;

    public abstract BorradorPublicacionDao borradorPublicacionDao();

    public abstract MiPublicacionDao miPublicacionDao();

    public abstract PublicacionEstadoDao publicacionEstadoDao();

    public abstract PublicacionGuardadaDao publicacionGuardadaDao();

    public abstract PreguntaDao preguntaDao();

    public abstract OfertaDao ofertaDao();

    public abstract PublicacionVistaDao publicacionVistaDao();

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
