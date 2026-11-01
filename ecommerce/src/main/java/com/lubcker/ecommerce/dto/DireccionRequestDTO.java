package com.lubcker.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DireccionRequestDTO {
    private int idCliente;
    @NotBlank(message = "La provincia es obligatoria.")
    private String provincia;

    @NotBlank(message = "La ciudad es obligatoria.")
    private String ciudad;

    @NotBlank(message = "La calle es obligatoria.")
    private String calle;

    @Positive(message = "La altura debe ser mayor a 0.")
    private int altura;
}