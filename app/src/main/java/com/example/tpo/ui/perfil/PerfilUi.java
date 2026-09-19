package com.example.tpo.ui.perfil;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.example.tpo.R;
import com.example.tpo.data.PerfilRepository;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.model.Reputacion;
import com.example.tpo.model.Usuario;
import com.example.tpo.util.TextoUtils;

import java.util.Locale;

/**
 * Helpers de UI compartidos por el perfil propio y el perfil público (Punto 2).
 * <p>
 * Mismo espíritu que {@code VendedorUi} y {@code ChipsUtils}: las dos pantallas
 * incluyen los mismos bloques ({@code view_avatar.xml} y
 * {@code view_reputacion.xml}), así que los pintan con el mismo código y no
 * pueden mostrar la reputación de dos formas distintas.
 */
public final class PerfilUi {

    private static final Locale LOCALE_AR = Locale.forLanguageTag("es-AR");

    private PerfilUi() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Pinta {@code view_reputacion.xml}.
     *
     * @param textoSinCalificaciones qué decir si todavía no hay calificaciones: en
     *                               el perfil propio se le habla al usuario ("no
     *                               recibiste"), en el público se habla de él.
     */
    public static void pintarReputacion(View bloque, Reputacion reputacion,
                                        @StringRes int textoSinCalificaciones) {
        View contenedorPromedio = bloque.findViewById(R.id.contenedorPromedio);
        TextView sinCalificaciones = bloque.findViewById(R.id.textoSinCalificaciones);

        // Un usuario sin calificaciones y uno con mala reputación darían el mismo
        // 0,0: se distinguen mostrando bloques distintos.
        boolean hayCalificaciones = reputacion.tieneCalificaciones();
        contenedorPromedio.setVisibility(hayCalificaciones ? View.VISIBLE : View.GONE);
        sinCalificaciones.setVisibility(hayCalificaciones ? View.GONE : View.VISIBLE);
        sinCalificaciones.setText(textoSinCalificaciones);

        if (hayCalificaciones) {
            RatingBar barra = bloque.findViewById(R.id.barraEstrellas);
            TextView promedio = bloque.findViewById(R.id.textoPromedio);
            TextView cantidad = bloque.findViewById(R.id.textoCantidadCalificaciones);
            barra.setRating((float) reputacion.getPromedioEstrellas());
            promedio.setText(String.format(LOCALE_AR, "%.1f", reputacion.getPromedioEstrellas()));
            cantidad.setText(bloque.getResources().getQuantityString(
                    R.plurals.perfil_cantidad_calificaciones,
                    reputacion.getCantidadCalificaciones(),
                    reputacion.getCantidadCalificaciones()));
        }

        TextView vendedor = bloque.findViewById(R.id.textoOperacionesVendedor);
        TextView comprador = bloque.findViewById(R.id.textoOperacionesComprador);
        vendedor.setText(String.valueOf(reputacion.getOperacionesComoVendedor()));
        comprador.setText(String.valueOf(reputacion.getOperacionesComoComprador()));
    }

    /**
     * Pinta {@code view_avatar.xml}: las iniciales enseguida y, si el usuario
     * tiene foto, la pide al repositorio y la muestra cuando llega. Si la foto
     * falla quedan las iniciales: no vale la pena un mensaje de error por un avatar.
     *
     * @param host Fragment dueño de la vista, para no tocarla si ya se destruyó
     *             cuando llega la respuesta.
     */
    public static void pintarAvatar(Fragment host, View avatar, Usuario usuario,
                                    PerfilRepository repositorio) {
        TextView iniciales = avatar.findViewById(R.id.textoIniciales);
        ImageView foto = avatar.findViewById(R.id.imagenFoto);
        iniciales.setText(TextoUtils.iniciales(usuario.getNombre()));

        if (!usuario.tieneFoto()) {
            foto.setImageDrawable(null);
            foto.setVisibility(View.GONE);
            iniciales.setVisibility(View.VISIBLE);
            return;
        }

        repositorio.obtenerFoto(usuario, new RepositorioCallback<Bitmap>() {
            @Override
            public void onExito(Bitmap imagen) {
                if (host.getView() == null) {
                    return; // la pantalla ya no existe
                }
                foto.setImageBitmap(imagen);
                foto.setVisibility(View.VISIBLE);
                iniciales.setVisibility(View.GONE);
            }

            @Override
            public void onError(String mensaje) {
                if (host.getView() == null) {
                    return;
                }
                foto.setVisibility(View.GONE);
                iniciales.setVisibility(View.VISIBLE);
            }
        });
    }
}
