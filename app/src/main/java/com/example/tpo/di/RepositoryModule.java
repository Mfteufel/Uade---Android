package com.example.tpo.di;

import android.content.Context;

import com.example.tpo.data.OperacionRepository;
import com.example.tpo.data.OperacionRepositoryApi;
import com.example.tpo.data.OperacionRepositoryMock;
import com.example.tpo.data.PerfilRepository;
import com.example.tpo.data.PerfilRepositoryApi;
import com.example.tpo.data.PerfilRepositoryMock;
import com.example.tpo.data.remote.OperacionApi;
import com.example.tpo.data.remote.UsuarioApi;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;

/**
 * Decide qué implementación de cada repositorio reciben las pantallas.
 * <p>
 * Es el único lugar que conoce las implementaciones concretas: los Fragments
 * piden la interfaz con {@code @Inject} y no saben si del otro lado hay datos
 * simulados o Retrofit. Mismo estilo que {@code login/NetworkModule}
 * ({@code @Module} + {@code @Provides} + {@code @Singleton}).
 * <p>
 * Las interfaces de Retrofit se crean desde el {@link Retrofit} de
 * {@code NetworkModule}: hay un solo cliente HTTP en toda la app y todos los
 * pedidos llevan el token JWT. No se arma ningún Retrofit acá.
 */
@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    /**
     * false = datos simulados ({@code BaseDeDatosMock}); true = API REST.
     * <p>
     * Cada repositorio tiene su propio switch. {@code /operaciones} (Punto 9)
     * necesita el backend de {@code feature/punto9-backend} desplegado: contra un
     * servidor sin esos endpoints el historial muestra el error, sin romperse.
     */
    private static final boolean USAR_API_PERFIL = true;
    private static final boolean USAR_API_OPERACIONES = true;

    @Provides
    @Singleton
    public PerfilRepository providePerfilRepository(@ApplicationContext Context context,
                                                    UsuarioApi api) {
        return USAR_API_PERFIL ? new PerfilRepositoryApi(api) : new PerfilRepositoryMock(context);
    }

    @Provides
    @Singleton
    public OperacionRepository provideOperacionRepository(OperacionApi api) {
        return USAR_API_OPERACIONES ? new OperacionRepositoryApi(api) : new OperacionRepositoryMock();
    }

    @Provides
    @Singleton
    public UsuarioApi provideUsuarioApi(Retrofit retrofit) {
        return retrofit.create(UsuarioApi.class);
    }

    @Provides
    @Singleton
    public OperacionApi provideOperacionApi(Retrofit retrofit) {
        return retrofit.create(OperacionApi.class);
    }
}
