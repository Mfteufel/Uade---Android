package com.example.tpo.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Base de datos Room de Ronda.
 * <p>
 * Por ahora solo tiene la tabla del borrador de "Publicar artículo" (Punto 5).
 * Se arma como singleton, igual que {@code PublicacionRepositoryMock} y
 * {@code SesionUsuario}, para no abrir más de una conexión a la misma base.
 */
@Database(entities = {BorradorPublicacionEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String NOMBRE_ARCHIVO = "ronda.db";

    private static volatile AppDatabase instancia;

    public abstract BorradorPublicacionDao borradorPublicacionDao();

    public static AppDatabase getInstancia(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    NOMBRE_ARCHIVO)
                            .build();
                }
            }
        }
        return instancia;
    }
}
