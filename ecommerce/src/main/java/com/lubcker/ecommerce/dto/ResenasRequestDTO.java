package com.lubcker.ecommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResenasRequestDTO {
    // Se ignora: la reseña siempre queda a nombre del usuario con sesion
    private int idCliente;

    private int idProducto;

    @Min(value = 1, message = "La valoración debe ser de 1 a 5 estrellas.")
    @Max(value = 5, message = "La valoración debe ser de 1 a 5 estrellas.")
    private int valoracion;

    @Size(max = 255, message = "El comentario puede tener hasta 255 caracteres.")
    private String descripcion;
}
