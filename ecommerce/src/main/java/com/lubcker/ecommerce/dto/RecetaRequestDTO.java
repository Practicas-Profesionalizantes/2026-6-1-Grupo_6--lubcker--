package com.lubcker.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecetaRequestDTO {

    // Lista vacia = el producto queda sin materia prima cargada
    @NotNull(message = "Faltan los insumos de la receta.")
    @Size(max = 50, message = "Una receta puede tener hasta 50 insumos.")
    @Valid
    private List<RecetaItemRequestDTO> items;
}
