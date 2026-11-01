package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClienteDTO {
    private int idCliente;
    private String nombre;
    private String apellido;
    private String telefono;
}