package com.example.tpo.di;

import android.content.Context;

import com.example.tpo.data.MisPublicacionesRepository;
import com.example.tpo.data.MisPublicacionesRepositoryApi;
import com.example.tpo.data.OfertasRepository;
import com.example.tpo.data.OfertasRepositoryRemoto;
import com.example.tpo.data.OperacionRepository;
import com.example.tpo.data.OperacionRepositoryApi;
import com.example.tpo.data.OperacionRepositoryMock;
import com.example.tpo.data.PerfilRepository;
import com.example.tpo.data.PerfilRepositoryApi;
import com.example.tpo.data.PerfilRepositoryMock;
import com.example.tpo.data.remote.OfertasApi;
import com.example.tpo.data.remote.OperacionApi;
import com.example.tpo.data.remote.PublicacionesApi;
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

    /**
     * Sin flag {@code USAR_API}: a diferencia de Perfil y Operaciones, el backend
     * de publicaciones (Punto 5) ya está levantado desde el día uno.
     */
    @Provides
    @Singleton
    public MisPublicacionesRepository provideMisPublicacionesRepository(
            @ApplicationContext Context context, PublicacionesApi api) {
        return new MisPublicacionesRepositoryApi(context, api);
    }

    @Provides
    @Singleton
    public PublicacionesApi providePublicacionesApi(Retrofit retrofit) {
        return retrofit.create(PublicacionesApi.class);
    }

    /**
     * Sin flag: el backend de ofertas (Punto 7, versión 0.4.0) ya está
     * levantado, no hay capa mock.
     */
    @Provides
    @Singleton
    public OfertasRepository provideOfertasRepository(OfertasApi api) {
        return new OfertasRepositoryRemoto(api);
    }

    @Provides
    @Singleton
    public OfertasApi provideOfertasApi(Retrofit retrofit) {
        return retrofit.create(OfertasApi.class);
    }
}
