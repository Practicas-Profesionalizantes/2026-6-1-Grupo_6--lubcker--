package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCheckoutDTO {
    private int idProducto;
    private int cantidad;
    // Medida elegida (ej: 5 = 5 kg). 0 o ausente = 1. El backend verifica que el producto se venda en esa medida.
    private int tamano;
}