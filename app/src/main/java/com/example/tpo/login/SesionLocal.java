package com.example.tpo.login;

import com.example.tpo.data.BusquedaGuardadaRepositoryMock;
import com.example.tpo.data.FavoritoRepositoryMock;
import com.example.tpo.data.RepositorioCallback;
import com.example.tpo.data.SesionUsuario;
import com.example.tpo.model.Zona;

public final class SesionLocal {

    private static final RepositorioCallback<Void> SIN_RESULTADO = new RepositorioCallback<Void>() {
        @Override
        public void onExito(Void resultado) {
        }

        @Override
        public void onError(String mensaje) {
        }
    };

    private SesionLocal() {
    }

    // deja en memoria quien inicio sesion, para que el resto de la app lo use
    public static void actualizar(SesionResponse.Usuario usuario) {
        SesionUsuario sesion = SesionUsuario.getInstancia();
        sesion.setUsuarioId(usuario.getId());
        sesion.setIdUsuario(usuario.getId());
        sesion.setNombre(usuario.getNombre());
        if (usuario.getZona() != null) {
            try {
                sesion.setZona(Zona.valueOf(usuario.getZona()));
            } catch (IllegalArgumentException e) {
                // zona desconocida: queda la que ya tenia
            }
        }
        FavoritoRepositoryMock.getInstancia().precargar(SIN_RESULTADO);
        BusquedaGuardadaRepositoryMock.getInstancia().precargar(SIN_RESULTADO);
    }
}
