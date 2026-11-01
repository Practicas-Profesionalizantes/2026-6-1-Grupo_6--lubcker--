package com.lubcker.ecommerce.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsumoRequestDTO {

    @NotBlank(message = "El nombre del insumo es obligatorio.")
    @Size(max = 100, message = "El nombre puede tener hasta 100 caracteres.")
    private String nombre;

    @NotBlank(message = "Elegí la unidad de medida del insumo.")
    private String unidadMedida;

    @NotNull(message = "El costo por unidad es obligatorio.")
    @DecimalMin(value = "0.01", message = "El costo por unidad tiene que ser mayor a 0.")
    @DecimalMax(value = "100000000", message = "El costo por unidad es demasiado alto.")
    private BigDecimal costoUnitario;

    @Size(max = 100, message = "El proveedor puede tener hasta 100 caracteres.")
    private String proveedor;
}
