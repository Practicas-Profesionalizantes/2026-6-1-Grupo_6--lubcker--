package com.lubcker.ecommerce.chat;

// El asistente no puede responder ahora (sin clave configurada, error del proveedor, etc.).
// El mensaje es para mostrarle al usuario: nunca incluye detalles tecnicos ni la clave.
public class ChatNoDisponibleException extends RuntimeException {
    public ChatNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
