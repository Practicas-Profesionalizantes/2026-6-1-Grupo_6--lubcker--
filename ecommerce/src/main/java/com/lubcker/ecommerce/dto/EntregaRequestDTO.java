package com.lubcker.ecommerce.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

// Lo que el dueño le comunica al cliente sobre la entrega. Todo es opcional: sin fecha, se borra la fecha.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntregaRequestDTO {
    // yyyy-MM-dd; vacio o null = sin fecha definida
    private String fechaEntrega;

    @Size(max = 50, message = "La franja horaria puede tener hasta 50 caracteres.")
    private String franjaHoraria;

    @Size(max = 255, message = "El mensaje puede tener hasta 255 caracteres.")
    private String notaCliente;
}
