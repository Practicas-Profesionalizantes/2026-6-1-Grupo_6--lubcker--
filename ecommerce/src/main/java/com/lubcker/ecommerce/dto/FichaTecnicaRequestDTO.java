package com.lubcker.ecommerce.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FichaTecnicaRequestDTO {
    private int idProducto;

    @Size(max = 50, message = "El modelo puede tener hasta 50 caracteres.")
    private String modelo;

    @Size(max = 100, message = "La presentación puede tener hasta 100 caracteres.")
    private String presentacion;

    @Size(max = 255, message = "La descripción puede tener hasta 255 caracteres.")
    private String descripcion;
}
