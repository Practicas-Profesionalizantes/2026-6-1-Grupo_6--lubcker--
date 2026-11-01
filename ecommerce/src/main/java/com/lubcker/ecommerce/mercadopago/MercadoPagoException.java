package com.lubcker.ecommerce.mercadopago;

// Error al hablar con Mercado Pago. El mensaje es apto para mostrarle al usuario.
public class MercadoPagoException extends RuntimeException {
    public MercadoPagoException(String mensaje) {
        super(mensaje);
    }
}
