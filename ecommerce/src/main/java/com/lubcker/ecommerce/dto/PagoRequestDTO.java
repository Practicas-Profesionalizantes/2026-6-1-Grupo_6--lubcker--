package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoRequestDTO {
    private int idOrden;
    private int idMetodoPago;
    private int monto;
    // estado no se pide: se crea siempre en false (pendiente) y se confirma
    // despues con PUT /api/pagos/{id}/confirmar
}