package com.lubcker.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

// Lo que el cliente (frontend) manda en el body al crear/editar un producto.
// Solo pide el id de la categoria, no el objeto completo.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequestDTO {
    @NotBlank(message = "El nombre del producto es obligatorio.")
    @Size(max = 100, message = "El nombre puede tener hasta 100 caracteres.")
    private String nombre;

    @Positive(message = "El precio debe ser mayor a 0.")
    private int precio;

    private int idCategoria;
    @Size(max = 255, message = "La ruta de la imagen es demasiado larga.")
    private String imagen;

    @PositiveOrZero(message = "El stock no puede ser negativo.")
    private int stock;

    // Unidad de medida: unidad, kg, litro o metro. null = no cambiar (al crear: "unidad")
    @Size(max = 20, message = "La unidad de medida es demasiado larga.")
    private String unidadMedida;

    // Medidas en las que se vende (ej: [3, 5, 10] kg). null = no cambiar (al crear: [1])
    private List<Integer> presentaciones;

    // null = no cambiar (al crear, queda activo)
    private Boolean activo;
}