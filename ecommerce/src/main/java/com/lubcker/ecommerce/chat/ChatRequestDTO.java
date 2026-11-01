package com.lubcker.ecommerce.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {

    // Ultimos mensajes de la conversacion (el ultimo es la pregunta nueva)
    @Valid
    @NotEmpty(message = "Escribí un mensaje.")
    @Size(max = 12, message = "La conversación es demasiado larga.")
    private List<MensajeChat> mensajes;
}
