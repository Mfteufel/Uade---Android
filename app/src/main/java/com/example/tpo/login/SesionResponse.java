package com.example.tpo.login;

public class SesionResponse {

    private String token;
    private Usuario usuario;

    public String getToken() {
        return token;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public static class Usuario {

        private String id;
        private String email;
        private String nombre;
        private String zona;

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String getNombre() {
            return nombre;
        }

        public String getZona() {
            return zona;
        }
    }
}
