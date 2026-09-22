package com.example.tpo.di;

import android.content.Context;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;

@EntryPoint
@InstallIn(SingletonComponent.class)
public interface RetrofitEntryPoint {

    Retrofit retrofit();

    static Retrofit obtenerRetrofit(Context context) {
        return EntryPointAccessors
                .fromApplication(context.getApplicationContext(), RetrofitEntryPoint.class)
                .retrofit();
    }
}
