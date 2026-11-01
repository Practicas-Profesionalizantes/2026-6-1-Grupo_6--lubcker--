package com.lubcker.ecommerce.service;

// Error de negocio durante la compra (carrito vacío, producto inexistente, etc.).
// Al ser RuntimeException, hace que la transacción se deshaga completa.
public class CheckoutException extends RuntimeException {
    public CheckoutException(String mensaje) {
        super(mensaje);
    }
}