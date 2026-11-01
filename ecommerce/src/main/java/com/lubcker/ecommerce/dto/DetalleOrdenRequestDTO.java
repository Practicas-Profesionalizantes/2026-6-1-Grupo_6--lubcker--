package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleOrdenRequestDTO {
    private int idOrden;
    private int idProducto;
    private int cantidad;
    private int precioUnitario;
    // subtotal NO se pide: lo calcula el backend (cantidad * precioUnitario)
}