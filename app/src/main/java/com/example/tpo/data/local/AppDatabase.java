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
 * real), tres tablas de persistencia del Detalle (Punto 4): preguntas,
 * ofertas y el override de estado de la publicación, y la de publicaciones
 * vistas (Punto 6, modo sin conexión). Se arma como singleton, igual que
 * {@code PublicacionRepositoryMock} y {@code SesionUsuario}, para no abrir
 * más de una conexión a la misma base.
 */
@Database(entities = {
        BorradorPublicacionEntity.class,
        PublicacionMiaEntity.class,
        PublicacionEstadoEntity.class,
        PreguntaEntity.class,
        OfertaEntity.class,
        PublicacionVistaEntity.class
}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String NOMBRE_ARCHIVO = "ronda.db";

    private static volatile AppDatabase instancia;

    public abstract BorradorPublicacionDao borradorPublicacionDao();

    public abstract MiPublicacionDao miPublicacionDao();

    public abstract PublicacionEstadoDao publicacionEstadoDao();

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
