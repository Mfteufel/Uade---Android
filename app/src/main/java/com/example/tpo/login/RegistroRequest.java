package com.example.tpo.login;

public class RegistroRequest {

    private final String nombre;
    private final String email;
    private final String password;

    public RegistroRequest(String nombre, String email, String password) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
    }
}
