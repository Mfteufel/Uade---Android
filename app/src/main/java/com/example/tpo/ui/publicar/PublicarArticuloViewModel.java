package com.example.tpo.ui.publicar;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.tpo.data.BorradorRepository;
import com.example.tpo.data.BorradorRepositoryLocal;
import com.example.tpo.data.MisPublicacionesRepository;
import com.example.tpo.data.PublicacionRepositoryMock;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.di.PublicarEntryPoint;
import com.example.tpo.model.BorradorPublicacion;
import com.example.tpo.model.MiPublicacion;

import dagger.hilt.android.EntryPointAccessors;

/**
 * Estado compartido entre los cinco pasos del wizard de "Publicar artículo" (Punto 5).
 * <p>
 * Vive con scope al sub nav graph del wizard (se obtiene con
 * {@code new ViewModelProvider(navController.getBackStackEntry(R.id.nav_graph_publicar))}),
 * así que sobrevive a la navegación entre pasos pero se destruye si el usuario
 * sale del wizard, igual que si hubiera un solo Fragment con cinco páginas.
 * <p>
 * El {@link BorradorPublicacion} que expone es mutable: cada Fragment del wizard
 * escribe directamente sobre el objeto que devuelve {@link #getBorrador()} y
 * después llama a {@link #guardarPaso(int)}, que lo persiste en Room (para poder
 * retomarlo si se cierra la app) y notifica a los observers.
 */
public class PublicarArticuloViewModel extends AndroidViewModel {

    private final BorradorRepository borradorRepository;
    private final MisPublicacionesRepository misPublicacionesRepository;

    private final MutableLiveData<BorradorPublicacion> borrador = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargandoBorrador = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> publicando = new MutableLiveData<>(false);

    public PublicarArticuloViewModel(@NonNull Application application) {
        super(application);
        borradorRepository = BorradorRepositoryLocal.getInstancia(application);
        // No es un @HiltViewModel (lo scopea a mano el nav graph del wizard), así
        // que no puede recibir el repositorio por @Inject directo: se pide por
        // EntryPoint, la forma estándar de Hilt para clases no administradas por
        // el framework (ver PublicarEntryPoint).
        PublicarEntryPoint entryPoint = EntryPointAccessors.fromApplication(
                application, PublicarEntryPoint.class);
        misPublicacionesRepository = entryPoint.misPublicacionesRepository();
        cargarBorradorGuardado();
    }

    /**
     * Busca en Room si había un borrador a medio hacer. Se llama una sola vez,
     * al crearse el ViewModel (o sea, la primera vez que se abre el wizard en
     * esta sesión de navegación), para no pisar cambios que el usuario ya hizo
     * en esta misma sesión con una consulta vieja.
     */
    private void cargarBorradorGuardado() {
        borradorRepository.obtener(new RepositorioCallback<BorradorPublicacion>() {
            @Override
            public void onExito(BorradorPublicacion resultado) {
                borrador.setValue(resultado != null ? resultado : new BorradorPublicacion());
                cargandoBorrador.setValue(false);
            }

            @Override
            public void onError(String mensaje) {
                // No hay borrador previo o falló la lectura: se arranca de cero,
                // no tiene sentido bloquear el wizard por esto.
                borrador.setValue(new BorradorPublicacion());
                cargandoBorrador.setValue(false);
            }
        });
    }

    public LiveData<BorradorPublicacion> getBorrador() {
        return borrador;
    }

    /** true mientras se está leyendo el borrador guardado de Room. Los pasos no deberían mostrarse hasta que sea false. */
    public LiveData<Boolean> getCargandoBorrador() {
        return cargandoBorrador;
    }

    public LiveData<Boolean> getPublicando() {
        return publicando;
    }

    /**
     * Persiste el borrador y marca en qué paso quedó. Cada Fragment del wizard
     * llama esto al tocar "Siguiente" o "Atrás", después de escribir sus propios
     * campos sobre el objeto de {@link #getBorrador()}.
     */
    public void guardarPaso(int paso) {
        BorradorPublicacion actual = borrador.getValue();
        if (actual == null) {
            return;
        }
        actual.setPaso(paso);
        borradorRepository.guardar(actual);
        // Mismo objeto, pero se vuelve a asignar para que los observers (por
        // ejemplo el resumen final) se refresquen con los datos nuevos.
        borrador.setValue(actual);
    }

    public boolean borradorCompleto() {
        BorradorPublicacion actual = borrador.getValue();
        return actual != null && actual.estaCompleto();
    }

    /** Sube el borrador y crea la publicación. Si sale bien, borra el borrador local. */
    public void publicar(RepositorioCallback<MiPublicacion> callback) {
        BorradorPublicacion actual = borrador.getValue();
        if (actual == null || !actual.estaCompleto()) {
            callback.onError("Completá todos los datos antes de publicar");
            return;
        }

        publicando.setValue(true);
        misPublicacionesRepository.publicar(actual, new RepositorioCallback<MiPublicacion>() {
            @Override
            public void onExito(MiPublicacion resultado) {
                publicando.setValue(false);
                borradorRepository.borrar();
                // "Mis publicaciones" (Room) y el catálogo de Home (mock en memoria)
                // son dos fuentes de datos separadas mientras no hay backend: sin
                // este paso, lo recién publicado nunca aparecía en Home. Ver
                // PublicacionRepositoryMock#agregarPublicacionDelUsuario.
                PublicacionRepositoryMock.getInstancia().agregarPublicacionDelUsuario(actual);
                callback.onExito(resultado);
            }

            @Override
            public void onError(String mensaje) {
                publicando.setValue(false);
                callback.onError(mensaje);
            }
        });
    }
}
