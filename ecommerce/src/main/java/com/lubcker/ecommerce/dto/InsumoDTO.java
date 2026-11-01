package com.lubcker.ecommerce.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsumoDTO {
    private int idInsumo;
    private String nombre;
    private String unidadMedida;
    private BigDecimal costoUnitario;
    private String proveedor;
    // En cuantos productos se usa (si es mayor a 0 no se puede borrar)
    private long productosQueLoUsan;
}
