package com.example.tpo.login;

public class CodigoRequest {

    private final String email;
    private final String codigo;

    // para pedir o reenviar un codigo alcanza con el email
    public CodigoRequest(String email) {
        this(email, null);
    }

    public CodigoRequest(String email, String codigo) {
        this.email = email;
        this.codigo = codigo;
    }
}
