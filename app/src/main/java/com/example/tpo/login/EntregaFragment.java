package com.example.tpo.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.tpo.R;
import com.example.tpo.util.FormatoUtils;
import com.google.android.material.appbar.MaterialToolbar;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Panel de una venta que espera la entrega: el comprador entra al escaner desde aca
// y el vendedor abre la pagina con el QR para imprimir.
@AndroidEntryPoint
public class EntregaFragment extends Fragment {

    public static final String ARG_OPERACION_ID = "operacionId";
    private static final String TAG = "Entrega";

    @Inject
    EntregaApi entregaApi;

    private String operacionId;

    private ProgressBar progreso;
    private TextView textoError;
    private View tarjeta;
    private TextView textoEstado;
    private TextView textoArticulo;
    private TextView textoPrecio;
    private TextView textoContraparte;
    private TextView textoAyuda;
    private Button botonAccion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrega, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        operacionId = requireArguments().getString(ARG_OPERACION_ID);

        progreso = view.findViewById(R.id.progreso);
        textoError = view.findViewById(R.id.textoError);
        tarjeta = view.findViewById(R.id.tarjeta);
        textoEstado = view.findViewById(R.id.textoEstado);
        textoArticulo = view.findViewById(R.id.textoArticulo);
        textoPrecio = view.findViewById(R.id.textoPrecio);
        textoContraparte = view.findViewById(R.id.textoContraparte);
        textoAyuda = view.findViewById(R.id.textoAyuda);
        botonAccion = view.findViewById(R.id.botonAccion);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbarEntrega);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        cargarEntrega();
    }

    private void cargarEntrega() {
        entregaApi.verEntrega(operacionId).enqueue(new Callback<EntregaResponse>() {
            @Override
            public void onResponse(@NonNull Call<EntregaResponse> call,
                                   @NonNull Response<EntregaResponse> response) {
                if (getView() == null) {
                    return;
                }
                progreso.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    mostrarEntrega(response.body());
                } else if (response.code() == 401) {
                    mostrarError(getString(R.string.login_sesion_vencida));
                } else if (response.code() == 404) {
                    mostrarError(getString(R.string.entrega_no_encontrada));
                } else {
                    mostrarError(getString(R.string.login_error_servidor, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<EntregaResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
                if (getView() == null) {
                    return;
                }
                progreso.setVisibility(View.GONE);
                mostrarError(getString(R.string.login_sin_conexion));
            }
        });
    }

    private void mostrarEntrega(EntregaResponse entrega) {
        boolean soyComprador = "COMPRA".equals(entrega.getTipo());
        boolean pendiente = "PENDIENTE_ENTREGA".equals(entrega.getEstado());

        textoEstado.setText(pendiente
                ? R.string.estado_publicacion_pendiente_entrega : R.string.entrega_confirmada);
        textoArticulo.setText(entrega.getArticulo());
        textoPrecio.setText(FormatoUtils.precio(entrega.getMontoFinal()));
        textoContraparte.setText(getString(
                soyComprador ? R.string.entrega_vendedor : R.string.entrega_comprador,
                entrega.getContraparte().getNombre()));
        tarjeta.setVisibility(View.VISIBLE);

        // con la entrega ya confirmada no queda nada para hacer: se muestran solo los datos
        if (!pendiente) {
            return;
        }
        textoAyuda.setText(soyComprador
                ? R.string.entrega_ayuda_comprador : R.string.entrega_ayuda_vendedor);
        textoAyuda.setVisibility(View.VISIBLE);
        botonAccion.setText(soyComprador
                ? R.string.entrega_escanear : R.string.entrega_ver_qr_imprimir);
        botonAccion.setVisibility(View.VISIBLE);
        botonAccion.setOnClickListener(v -> {
            if (soyComprador) {
                Bundle argumentos = new Bundle();
                argumentos.putString(ARG_OPERACION_ID, operacionId);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_entrega_to_escaner, argumentos);
            } else {
                // la pagina del QR la arma el servidor: se abre en el navegador con un intent implicito
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(entrega.getEnlaceQr())));
            }
        });
    }

    private void mostrarError(String mensaje) {
        textoError.setText(mensaje);
        textoError.setVisibility(View.VISIBLE);
    }
}
