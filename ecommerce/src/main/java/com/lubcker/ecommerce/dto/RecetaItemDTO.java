package com.lubcker.ecommerce.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecetaItemDTO {
    private int idInsumo;
    private String nombreInsumo;
    private String unidadMedida;
    private BigDecimal costoUnitario;
    // Cantidad por UNA unidad del producto
    private BigDecimal cantidad;
    // cantidad x costoUnitario
    private BigDecimal costo;
}
