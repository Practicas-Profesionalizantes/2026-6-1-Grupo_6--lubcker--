package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleOrdenDTO {
    private int idDetalle;
    private int idOrden;
    private int idProducto;
    private String nombreProducto;
    private int cantidad;
    private int precioUnitario;
    private int subtotal;
    private int tamano;
    private String unidadMedida;
}