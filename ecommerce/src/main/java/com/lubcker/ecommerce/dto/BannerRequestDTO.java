package com.lubcker.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BannerRequestDTO {

    @NotBlank(message = "Subí una imagen para el banner.")
    @Size(max = 255, message = "La ruta de la imagen es demasiado larga.")
    private String imagen;

    @Size(max = 100, message = "El texto puede tener hasta 100 caracteres.")
    private String titulo;

    @Size(max = 255, message = "El enlace es demasiado largo.")
    private String enlace;

    // null = no cambiar (al crear queda activo)
    private Boolean activo;
}
