package com.lubcker.ecommerce.mercadopago;

// Resultado de consultar un pago a Mercado Pago y reflejarlo en el pedido.
// estadoMp: approved, pending, in_process, rejected, cancelled, refunded...
public record ResultadoPago(int idOrden, String estadoMp, boolean confirmado) {
}
