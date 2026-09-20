package com.example.tpo.data.remote;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Traduce una respuesta HTTP fallida a un mensaje listo para mostrar.
 * <p>
 * FastAPI responde los errores como {@code {"detail": "mensaje"}} (400, 403,
 * 404, 409); si viene así, se muestra ese texto, que ya está pensado para el
 * usuario. Los 422 de validación traen {@code detail} como lista técnica y se
 * reemplazan por el mensaje por defecto de cada pantalla.
 */
public final class ErrorApi {

    /** Para {@code onFailure}: no hubo respuesta (sin red, servidor caído, timeout). */
    public static final String SIN_CONEXION =
            "No pudimos conectarnos. Revisá tu conexión e intentá de nuevo.";

    private static final String SESION_VENCIDA = "Tu sesión venció. Volvé a iniciar sesión.";

    private ErrorApi() {
        // Clase de utilidades: no se instancia.
    }

    public static String mensaje(Response<?> respuesta, String porDefecto) {
        if (respuesta.code() == 401) {
            return SESION_VENCIDA;
        }
        String detalle = detalle(respuesta);
        return detalle != null ? detalle : porDefecto;
    }

    /**
     * Igual que {@link #mensaje}, pero sin pisar el 401 con "sesión vencida": lo
     * usa el login/OTP, donde un 401 significa "código o contraseña incorrectos"
     * y todavía no hay ninguna sesión que pueda haber vencido.
     */
    public static String detalle(Response<?> respuesta) {
        return leerDetalle(respuesta.errorBody());
    }

    private static String leerDetalle(ResponseBody cuerpo) {
        if (cuerpo == null) {
            return null;
        }
        try {
            JsonElement json = JsonParser.parseString(cuerpo.string());
            if (!json.isJsonObject()) {
                return null;
            }
            JsonObject objeto = json.getAsJsonObject();
            JsonElement detalle = objeto.get("detail");
            if (detalle != null && detalle.isJsonPrimitive()) {
                return detalle.getAsString();
            }
            return null;
        } catch (IOException | RuntimeException excepcion) {
            // Cuerpo vacío o que no es JSON (por ejemplo una página de error del proxy).
            return null;
        }
    }
}
