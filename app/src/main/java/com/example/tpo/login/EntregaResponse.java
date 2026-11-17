package com.example.tpo.login;

public class EntregaResponse {

    private String articulo;
    private double montoFinal;
    // COMPRA o VENTA, segun quien pregunta
    private String tipo;
    // PENDIENTE_ENTREGA o ENTREGADA
    private String estado;
    private Contraparte contraparte;
    // pagina con el QR para imprimir: solo viene si soy el vendedor y falta la entrega
    private String enlaceQr;

    public String getArticulo() {
        return articulo;
    }

    public double getMontoFinal() {
        return montoFinal;
    }

    public String getTipo() {
        return tipo;
    }

    public String getEstado() {
        return estado;
    }

    public Contraparte getContraparte() {
        return contraparte;
    }

    public String getEnlaceQr() {
        return enlaceQr;
    }

    public static class Contraparte {

        private String nombre;

        public String getNombre() {
            return nombre;
        }
    }
}
