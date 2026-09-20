package com.example.tpo.di;

import android.content.Context;

import androidx.room.Room;

import com.example.tpo.data.local.PublicacionVistaDao;
import com.example.tpo.data.local.RondaDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class StorageModule {

    @Provides
    @Singleton
    public RondaDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, RondaDatabase.class, "ronda_db").build();
    }

    @Provides
    @Singleton
    public PublicacionVistaDao providePublicacionVistaDao(RondaDatabase database) {
        return database.publicacionVistaDao();
    }
}
