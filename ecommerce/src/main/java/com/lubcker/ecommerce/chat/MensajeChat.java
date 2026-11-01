package com.lubcker.ecommerce.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MensajeChat {

    // Solo "user" o "assistant": el cliente NO puede inyectar mensajes de sistema
    @Pattern(regexp = "user|assistant", message = "Rol de mensaje inválido.")
    private String rol;

    @NotBlank(message = "El mensaje no puede estar vacío.")
    // El limite de 500 caracteres para el usuario se verifica en ChatController;
    // las respuestas del asistente (que vuelven como historial) pueden ser mas largas.
    @Size(max = 3000, message = "El mensaje es demasiado largo.")
    private String texto;
}
