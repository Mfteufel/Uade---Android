package com.example.tpo.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * Base de datos local de Ronda.
 */
@Database(entities = {PublicacionVistaEntity.class}, version = 1, exportSchema = false)
public abstract class RondaDatabase extends RoomDatabase {
    public abstract PublicacionVistaDao publicacionVistaDao();
}
