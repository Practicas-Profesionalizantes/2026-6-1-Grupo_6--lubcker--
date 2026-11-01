package com.lubcker.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroRequestDTO {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre es demasiado largo.")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(max = 100, message = "El apellido es demasiado largo.")
    private String apellido;

    @NotBlank(message = "El teléfono es obligatorio.")
    @Pattern(regexp = "^[+0-9][0-9 ()-]{6,19}$", message = "El teléfono no es válido. Ingresalo con el código de área, por ejemplo 11 4430-6194.")
    private String telefono;

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El correo electrónico no es válido.")
    @Size(max = 100, message = "El correo electrónico es demasiado largo.")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
    @Pattern(regexp = ".*\\d.*", message = "La contraseña debe incluir al menos un número.")
    private String contrasena;

    // Segundo paso del registro: datos del negocio
    @Valid
    @NotNull(message = "Completá los datos de tu negocio.")
    private NegocioDTO negocio;
}
