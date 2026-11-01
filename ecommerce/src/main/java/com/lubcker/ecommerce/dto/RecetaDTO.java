package com.lubcker.ecommerce.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

// Materia prima de un producto con su costo de fabricacion y ganancia, todo POR UNIDAD del producto
// (1 kg, 1 litro, 1 unidad...). Se calcula con los precios actuales de los insumos.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecetaDTO {
    private int idProducto;
    private String nombreProducto;
    private String unidadMedida;
    private int precioVenta;
    private List<RecetaItemDTO> items;
    private BigDecimal costoFabricacion;
    private BigDecimal ganancia;
    // ganancia / precio de venta, en %. null si el precio de venta es 0
    private BigDecimal margenPorcentaje;
}
