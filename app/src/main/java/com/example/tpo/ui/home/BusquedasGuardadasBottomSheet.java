package com.example.tpo.ui.home;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpo.R;
import com.example.tpo.data.BusquedaGuardadaRepository;
import com.example.tpo.data.BusquedaGuardadaRepositoryApi;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.BusquedaGuardada;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

/**
 * Hoja inferior con las búsquedas guardadas
 */
public class BusquedasGuardadasBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "BusquedasGuardadasBottomSheet";
    public static final String RESULTADO_BUSQUEDA_GUARDADA = "resultado_busqueda_guardada";
    public static final String EXTRA_FILTRO = "filtro";
    public static final String EXTRA_ID = "id";
    public static final String RESULTADO_CERRADA = "resultado_busquedas_guardadas_cerrada";

    private RecyclerView lista;
    private CircularProgressIndicator progreso;
    private View estadoVacio;

    private BusquedaGuardadaAdapter adapter;
    private BusquedaGuardadaRepository repositorio;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        repositorio = BusquedaGuardadaRepositoryApi.getInstancia(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_busquedas_guardadas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lista = view.findViewById(R.id.listaBusquedasGuardadas);
        progreso = view.findViewById(R.id.progresoBusquedasGuardadas);
        estadoVacio = view.findViewById(R.id.estadoVacioBusquedasGuardadas);

        adapter = new BusquedaGuardadaAdapter(repositorio, this::elegir, this::eliminar);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        lista.setAdapter(adapter);

        cargar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        lista = null;
        progreso = null;
        estadoVacio = null;
        adapter = null;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // Ver esta hoja (la lista de nombres) no cuenta como haber revisado
        // ninguna búsqueda puntual, eso se marca recién cuando el usuario la
        // aplica de verdad.
        // Acá solo se avisa al Home para que actualice el ícono con lo que haya
        // quedado, puede no haber cambiado nada si no se eligió ninguna.
        getParentFragmentManager().setFragmentResult(RESULTADO_CERRADA, new Bundle());
    }

    private void cargar() {
        progreso.setVisibility(View.VISIBLE);
        lista.setVisibility(View.GONE);
        estadoVacio.setVisibility(View.GONE);

        repositorio.listar(new RepositorioCallback<List<BusquedaGuardada>>() {
            @Override
            public void onExito(List<BusquedaGuardada> busquedas) {
                if (lista == null) {
                    return; // la vista ya no existe
                }
                progreso.setVisibility(View.GONE);
                adapter.reemplazar(busquedas);

                boolean sinBusquedas = busquedas.isEmpty();
                estadoVacio.setVisibility(sinBusquedas ? View.VISIBLE : View.GONE);
                lista.setVisibility(sinBusquedas ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(String mensaje) {
                if (lista == null) {
                    return;
                }
                // No vale la pena dejar la hoja abierta sin datos para mostrar.
                dismiss();
            }
        });
    }

    /** El usuario tocó una búsqueda, se la devuelve al Home y se cierra la hoja. */
    private void elegir(BusquedaGuardada busqueda) {
        Bundle resultado = new Bundle();
        resultado.putSerializable(EXTRA_FILTRO, busqueda.getFiltro());
        resultado.putString(EXTRA_ID, busqueda.getId());
        getParentFragmentManager().setFragmentResult(RESULTADO_BUSQUEDA_GUARDADA, resultado);
        dismiss();
    }

    private void eliminar(BusquedaGuardada busqueda) {
        repositorio.eliminar(busqueda.getId(), new RepositorioCallback<Void>() {
            @Override
            public void onExito(Void resultado) {
                cargar(); // se recarga para reflejar la baja
            }

            @Override
            public void onError(String mensaje) {
                // Nada especial: la próxima recarga la vuelve a mostrar igual.
            }
        });
    }
}
