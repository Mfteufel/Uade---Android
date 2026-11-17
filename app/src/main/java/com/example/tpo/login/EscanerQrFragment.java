package com.example.tpo.login;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.tpo.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Lee con la camara el QR que muestra el vendedor y le manda ese texto al servidor,
// que es el que decide si corresponde a esta venta.
@AndroidEntryPoint
public class EscanerQrFragment extends Fragment {

    private static final String TAG = "EscanerQr";

    @Inject
    EntregaApi entregaApi;

    private String operacionId;
    // mientras viaja un codigo o hay un resultado en pantalla no se manda otro
    private boolean enviando;
    private boolean confirmada;

    private PreviewView previewView;
    private TextView textoAyuda;
    private View panelResultado;
    private TextView textoIcono;
    private TextView textoTitulo;
    private TextView textoMensaje;
    private Button botonResultado;

    // hilo aparte para analizar las imagenes sin trabar la pantalla
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        operacionId = requireArguments().getString(EntregaFragment.ARG_OPERACION_ID);

        // el pedido de permiso se registra aca, antes de que el fragment arranque
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        iniciarCamara();
                    } else {
                        Toast.makeText(requireContext(), R.string.escaner_sin_permiso,
                                Toast.LENGTH_LONG).show();
                    }
                });

        // solo QR: asi descarta los demas formatos de codigo
        BarcodeScannerOptions opciones = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(opciones);

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView = view.findViewById(R.id.previewView);
        textoAyuda = view.findViewById(R.id.textoAyuda);
        panelResultado = view.findViewById(R.id.panelResultado);
        textoIcono = view.findViewById(R.id.textoIcono);
        textoTitulo = view.findViewById(R.id.textoTitulo);
        textoMensaje = view.findViewById(R.id.textoMensaje);
        botonResultado = view.findViewById(R.id.botonResultado);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbarEscaner);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        botonResultado.setOnClickListener(v -> cerrarResultado());

        pedirPermisoCamara();
    }

    private void pedirPermisoCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            // la respuesta llega al callback registrado en onCreate
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void iniciarCamara() {
        ListenableFuture<ProcessCameraProvider> futuro =
                ProcessCameraProvider.getInstance(requireContext());

        futuro.addListener(() -> {
            // si la pantalla se cerro mientras se preparaba la camara no hay donde mostrarla
            if (getView() == null) {
                return;
            }
            try {
                ProcessCameraProvider cameraProvider = futuro.get();

                // vista previa: lo que ve la camara se dibuja en el PreviewView
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // analisis: cada imagen pasa por analizarFrame; si llega una nueva
                // antes de terminar con la anterior, se queda solo con la ultima
                ImageAnalysis analisis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analisis.setAnalyzer(cameraExecutor, this::analizarFrame);

                CameraSelector camara = CameraSelector.DEFAULT_BACK_CAMERA;

                // la camara queda atada al ciclo de vida de la vista: se libera sola al salir
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), camara, preview, analisis);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error al iniciar la camara", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void analizarFrame(@NonNull ImageProxy imageProxy) {
        InputImage imagen = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(imagen)
                .addOnSuccessListener(codigos -> {
                    for (Barcode codigo : codigos) {
                        String valor = codigo.getRawValue();
                        if (valor != null) {
                            procesarResultado(valor);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error al leer el QR", e))
                // la imagen se cierra siempre: si no, la camara no entrega la siguiente
                .addOnCompleteListener(tarea -> imageProxy.close());
    }

    // La camara lee el mismo QR en cada imagen: con la bandera se manda una sola vez.
    private void procesarResultado(String rawValue) {
        if (enviando || getView() == null) {
            return;
        }
        enviando = true;
        textoAyuda.setText(R.string.escaner_verificando);

        entregaApi.confirmarEntrega(operacionId, new CodigoEntregaRequest(rawValue))
                .enqueue(new Callback<EntregaResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<EntregaResponse> call,
                                           @NonNull Response<EntregaResponse> response) {
                        if (getView() == null) {
                            return;
                        }
                        if (response.isSuccessful() && response.body() != null) {
                            mostrarResultado(true, R.string.escaner_confirmada_titulo,
                                    getString(R.string.escaner_confirmada_mensaje));
                        } else if (response.code() == 400) {
                            mostrarResultado(false, R.string.escaner_invalido_titulo,
                                    getString(R.string.escaner_invalido_mensaje));
                        } else if (response.code() == 409) {
                            mostrarResultado(true, R.string.escaner_confirmada_titulo,
                                    getString(R.string.escaner_ya_confirmada_mensaje));
                        } else if (response.code() == 401) {
                            mostrarResultado(false, R.string.escaner_error_titulo,
                                    getString(R.string.login_sesion_vencida));
                        } else if (response.code() == 404) {
                            mostrarResultado(false, R.string.escaner_error_titulo,
                                    getString(R.string.entrega_no_encontrada));
                        } else {
                            mostrarResultado(false, R.string.escaner_error_titulo,
                                    getString(R.string.login_error_servidor, response.code()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<EntregaResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Error de red: " + t.getMessage());
                        if (getView() == null) {
                            return;
                        }
                        mostrarResultado(false, R.string.escaner_error_titulo,
                                getString(R.string.login_sin_conexion));
                    }
                });
    }

    // Resultado a pantalla completa: verde si se confirmo, rojo si no.
    private void mostrarResultado(boolean confirmada, int titulo, String mensaje) {
        this.confirmada = confirmada;
        int color = ContextCompat.getColor(requireContext(),
                confirmada ? R.color.ronda_primary : R.color.ronda_error);
        textoIcono.setText(confirmada ? "✓" : "✕");
        textoIcono.setTextColor(color);
        textoTitulo.setText(titulo);
        textoTitulo.setTextColor(color);
        textoMensaje.setText(mensaje);
        botonResultado.setText(confirmada ? R.string.escaner_volver : R.string.escaner_reintentar);
        panelResultado.setVisibility(View.VISIBLE);
    }

    private void cerrarResultado() {
        if (confirmada) {
            Navigation.findNavController(requireView()).navigate(R.id.action_escaner_to_historial);
        } else {
            // se saca el cartel y la camara vuelve a mandar lo que lea
            panelResultado.setVisibility(View.GONE);
            textoAyuda.setText(R.string.escaner_ayuda);
            enviando = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }
}
