package com.lubcker.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

// Datos del negocio: se usa tanto para recibir (registro, perfil) como para devolver.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NegocioDTO {

    @NotBlank(message = "El nombre del negocio es obligatorio.")
    @Size(max = 100, message = "El nombre del negocio puede tener hasta 100 caracteres.")
    private String nombre;

    @NotBlank(message = "Elegí el rubro de tu negocio.")
    private String rubro;

    @NotBlank(message = "La provincia del negocio es obligatoria.")
    @Size(max = 100, message = "La provincia es demasiado larga.")
    private String provincia;

    @NotBlank(message = "La ciudad del negocio es obligatoria.")
    @Size(max = 100, message = "La ciudad es demasiado larga.")
    private String ciudad;

    @NotBlank(message = "La calle del negocio es obligatoria.")
    @Size(max = 100, message = "La calle es demasiado larga.")
    private String calle;

    @Positive(message = "La altura de la dirección del negocio debe ser mayor a 0.")
    private int altura;
}
