package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResenaGeneralDTO {
    private int idResenaGeneral;
    private int idCliente;
    // Nombre + inicial del apellido (no se publica el apellido completo)
    private String nombreCliente;
    private int valoracion;
    private String descripcion;
}
