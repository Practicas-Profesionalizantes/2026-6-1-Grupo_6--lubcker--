package com.lubcker.ecommerce.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecetaItemRequestDTO {

    private int idInsumo;

    @NotNull(message = "Completá la cantidad de cada insumo.")
    @DecimalMin(value = "0.0001", message = "La cantidad de cada insumo tiene que ser mayor a 0.")
    @DecimalMax(value = "1000000", message = "La cantidad de un insumo es demasiado grande.")
    private BigDecimal cantidad;
}
