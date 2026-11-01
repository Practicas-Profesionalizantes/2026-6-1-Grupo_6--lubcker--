package com.lubcker.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambioContrasenaDTO {

    @NotBlank(message = "La nueva contraseña es obligatoria.")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
    @Pattern(regexp = ".*\\d.*", message = "La contraseña debe incluir al menos un número.")
    private String nueva;
}
