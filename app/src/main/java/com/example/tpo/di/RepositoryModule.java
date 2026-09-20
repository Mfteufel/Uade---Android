package com.example.tpo.di;

import android.content.Context;

import com.example.tpo.data.AuthRepository;
import com.example.tpo.data.AuthRepositoryApi;
import com.example.tpo.data.OperacionRepository;
import com.example.tpo.data.OperacionRepositoryApi;
import com.example.tpo.data.OperacionRepositoryMock;
import com.example.tpo.data.PerfilRepository;
import com.example.tpo.data.PerfilRepositoryApi;
import com.example.tpo.data.PerfilRepositoryMock;
import com.example.tpo.data.remote.AuthApi;
import com.example.tpo.data.remote.OperacionApi;
import com.example.tpo.data.remote.UsuarioApi;
import com.example.tpo.login.TokenManager;

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
     * Queda en false hasta que el backend de Walter esté levantado. Antes de
     * pasarlo a true, seguir la lista de {@code docs/contrato-api-perfil-historial.md}
     * (permiso INTERNET, URL base, login que guarde el token).
     */
    private static final boolean USAR_API = false;

    @Provides
    @Singleton
    public PerfilRepository providePerfilRepository(@ApplicationContext Context context,
                                                    UsuarioApi api) {
        return USAR_API ? new PerfilRepositoryApi(api, context) : new PerfilRepositoryMock(context);
    }

    @Provides
    @Singleton
    public OperacionRepository provideOperacionRepository(OperacionApi api) {
        return USAR_API ? new OperacionRepositoryApi(api) : new OperacionRepositoryMock();
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

    @Provides
    @Singleton
    public AuthApi provideAuthApi(Retrofit retrofit) {
        return retrofit.create(AuthApi.class);
    }

    /**
     * Sin flag {@code USAR_API}: a diferencia de Perfil y Operaciones, el login
     * ya habla con el backend real desde el día uno (ver {@code AuthRepository}).
     */
    @Provides
    @Singleton
    public AuthRepository provideAuthRepository(AuthApi api, TokenManager tokenManager) {
        return new AuthRepositoryApi(api, tokenManager);
    }
}
