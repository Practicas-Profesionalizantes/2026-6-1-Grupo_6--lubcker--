package com.lubcker.ecommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResenaGeneralRequestDTO {
    @Min(value = 1, message = "La valoración debe ser de 1 a 5 estrellas.")
    @Max(value = 5, message = "La valoración debe ser de 1 a 5 estrellas.")
    private int valoracion;

    @Size(max = 255, message = "El comentario puede tener hasta 255 caracteres.")
    private String descripcion;
}
