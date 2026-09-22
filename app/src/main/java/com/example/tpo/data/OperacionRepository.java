package com.example.tpo.data;

import androidx.annotation.Nullable;

import com.example.tpo.model.FiltroOperaciones;
import com.example.tpo.model.Operacion;

import java.util.List;

/**
 * Fuente de datos del historial de operaciones y de las calificaciones (Punto 9).
 * <p>
 * Calificar vive acá y no en un repositorio aparte porque siempre se califica
 * <em>una operación</em>: el servidor valida contra ella (quién participó, cuándo
 * se entregó, si ya se calificó). Las calificaciones <em>recibidas</em> por una
 * persona, en cambio, son parte de su perfil y están en {@link PerfilRepository}.
 * <p>
 * Hilt decide qué implementación se inyecta ({@code di/RepositoryModule}):
 * {@link OperacionRepositoryApi} contra el backend o {@link OperacionRepositoryMock}.
 * Todas las respuestas llegan por callback en el Main Thread.
 */
public interface OperacionRepository {

    /**
     * Historial: operaciones concretadas (con la entrega confirmada) del usuario
     * logueado, vistas desde su lado (cada una trae si fue COMPRA o VENTA, la
     * contraparte y si se puede calificar). Equivale a
     * {@code GET /operaciones?tipo=&desde=&hasta=}.
     */
    void obtenerHistorial(FiltroOperaciones filtro, RepositorioCallback<List<Operacion>> callback);

    /**
     * Ventas aceptadas del usuario logueado que todavía esperan que el comprador
     * confirme la entrega. No son parte del historial y no se filtran. Equivale a
     * {@code GET /operaciones/pendientes}.
     */
    void obtenerPendientesDeEntrega(RepositorioCallback<List<Operacion>> callback);

    /**
     * El comprador confirma que recibió el artículo: la operación queda concretada
     * y desde ese momento corren los 7 días para calificar. El servidor rechaza al
     * vendedor, a un tercero y una segunda confirmación. Equivale a
     * {@code POST /operaciones/{id}/entrega}.
     *
     * @return por callback, la operación actualizada (ya {@code ENTREGADA}).
     */
    void confirmarEntrega(String operacionId, RepositorioCallback<Operacion> callback);

    /**
     * Califica a la contraparte de una operación. El servidor es el que valida
     * todas las reglas (participante, entregada, dentro de los 7 días, una sola
     * vez, 1 a 5 estrellas); si alguna falla, llega por {@code onError} con el
     * mensaje listo para mostrar. Equivale a {@code POST /operaciones/{id}/calificacion}.
     *
     * @param comentario opcional; {@code null} o vacío si no escribió nada.
     * @return por callback, la operación actualizada (ya con {@code miCalificacion}).
     */
    void calificar(String operacionId, int estrellas, @Nullable String comentario,
                   RepositorioCallback<Operacion> callback);
}
