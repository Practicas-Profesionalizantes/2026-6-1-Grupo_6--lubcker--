package com.lubcker.ecommerce.dto;

import lombok.*;

import java.math.BigDecimal;

// Costo y ganancia por unidad de un producto que ya tiene materia prima cargada
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumenCostoDTO {
    private int idProducto;
    private int precioVenta;
    private BigDecimal costoFabricacion;
    private BigDecimal ganancia;
    private BigDecimal margenPorcentaje;
}
