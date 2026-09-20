package com.example.tpo.data;

/**
 * Login y registro (Punto 1): OTP por email o usuario/contraseña, contra el
 * backend real — a diferencia del resto de los repositorios, no tiene mock ni
 * depende de {@code RepositoryModule.USAR_API}, porque {@code /auth/*} ya está
 * levantado y probado.
 */
public interface AuthRepository {

    /** Pide un código nuevo. El mensaje de éxito trae cuántos segundos dura. */
    void solicitarCodigo(String email, RepositorioCallback<String> callback);

    /** Igual que {@link #solicitarCodigo}, pero el backend rechaza si pasaron menos de 30s. */
    void reenviarCodigo(String email, RepositorioCallback<String> callback);

    /** Verifica el código: si es válido, guarda el token e hidrata {@link SesionUsuario}. */
    void verificarCodigo(String email, String codigo, RepositorioCallback<Void> callback);

    /** Login clásico: si las credenciales son correctas, guarda el token e hidrata {@link SesionUsuario}. */
    void iniciarSesionConPassword(String email, String password, RepositorioCallback<Void> callback);

    /**
     * Recupera quién es el usuario del token ya guardado ({@code GET /auth/sesion}),
     * sin pedir credenciales de nuevo. La usa el desbloqueo biométrico: el token
     * sobrevive a que se mate el proceso, pero {@link SesionUsuario} no.
     */
    void restaurarSesion(RepositorioCallback<Void> callback);
}
