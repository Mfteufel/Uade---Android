package com.example.tpo.data;

import com.example.tpo.model.Oferta;

import java.util.ArrayList;
import java.util.List;

/**
 * Ofertas de precio que los interesados le hicieron a los vendedores, en
 * memoria. Mismo espíritu que {@link PreguntasPublicacion}: singleton que se
 * pierde al cerrar la app, reemplazable el día que exista persistencia real o
 * la API ({@code POST /publicaciones/{id}/ofertas}).
 * <p>
 * <b>Regla propia de esta clase:</b> cada usuario tiene como máximo <em>una</em>
 * oferta vigente por publicación. Volver a ofertar no apila una oferta nueva,
 * reemplaza la anterior — ofertar de nuevo es mejorar (o cambiar) el número
 * que ya mandaste, no mandar una segunda propuesta en paralelo.
 */
public class OfertasPublicacion {

    private static OfertasPublicacion instancia;

    private final List<Oferta> ofertas = new ArrayList<>();

    private OfertasPublicacion() {
        // Constructor privado: se accede siempre por getInstancia().
    }

    public static synchronized OfertasPublicacion getInstancia() {
        if (instancia == null) {
            instancia = new OfertasPublicacion();
        }
        return instancia;
    }

    /** Guarda la oferta, reemplazando la anterior del mismo usuario para esa publicación si existía. */
    public void guardar(Oferta oferta) {
        Oferta anterior = delUsuario(oferta.getPublicacionId(), oferta.getAutorId());
        if (anterior != null) {
            ofertas.remove(anterior);
        }
        ofertas.add(oferta);
    }

    /** Todas las ofertas de una publicación, para que las vea su vendedor en la gestión. */
    public List<Oferta> deLaPublicacion(String publicacionId) {
        List<Oferta> resultado = new ArrayList<>();
        for (Oferta oferta : ofertas) {
            if (oferta.getPublicacionId().equals(publicacionId)) {
                resultado.add(oferta);
            }
        }
        return resultado;
    }

    /** La oferta vigente de un usuario para una publicación, o {@code null} si no hizo ninguna. */
    public Oferta delUsuario(String publicacionId, String usuarioId) {
        for (Oferta oferta : ofertas) {
            if (oferta.getPublicacionId().equals(publicacionId)
                    && oferta.getAutorId().equals(usuarioId)) {
                return oferta;
            }
        }
        return null;
    }

    public int cantidadEn(String publicacionId) {
        return deLaPublicacion(publicacionId).size();
    }
}
