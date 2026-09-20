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
 * real) y las cuatro tablas de persistencia del Detalle (Punto 4): guardados,
 * preguntas, ofertas (con su ciclo de negociación completo del Punto 7) y el
 * override de estado de la publicación. Se arma como singleton, igual que
 * {@code PublicacionRepositoryMock} y {@code SesionUsuario}, para no abrir
 * más de una conexión a la misma base.
 * <p>
 * {@code version = 5}: subió dos veces en paralelo (dos ramas distintas
 * llevaron {@code OfertaEntity} de v3 a v4, cada una con columnas propias) y
 * quedaron esquemas incompatibles bajo el mismo número. Sin este bump, un
 * dispositivo que ya haya corrido una de las dos v4 crashea al abrir la app
 * con la otra ({@code IllegalStateException} de Room por hash de esquema
 * distinto) en vez de simplemente recrear la base, que es lo que hace
 * {@link androidx.room.RoomDatabase.Builder#fallbackToDestructiveMigration()}
 * — pero solo ante un cambio de versión, no si el número quedó pisado.
 */
@Database(entities = {
        BorradorPublicacionEntity.class,
        PublicacionMiaEntity.class,
        PublicacionEstadoEntity.class,
        PublicacionGuardadaEntity.class,
        PreguntaEntity.class,
        OfertaEntity.class
}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String NOMBRE_ARCHIVO = "ronda.db";

    private static volatile AppDatabase instancia;

    public abstract BorradorPublicacionDao borradorPublicacionDao();

    public abstract MiPublicacionDao miPublicacionDao();

    public abstract PublicacionEstadoDao publicacionEstadoDao();

    public abstract PublicacionGuardadaDao publicacionGuardadaDao();

    public abstract PreguntaDao preguntaDao();

    public abstract OfertaDao ofertaDao();

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
