package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {
    private int idPago;
    private int idOrden;
    private int idMetodoPago;
    private String nombreMetodoPago;
    private boolean estado;
    private int monto;
}