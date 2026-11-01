package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResenasDTO {
    private int idResenas;
    private int idCliente;
    // Nombre + inicial del apellido (no se publica el apellido completo)
    private String nombreCliente;
    private int idProducto;
    private String nombreProducto;
    private String imagenProducto;
    private int valoracion;
    private String descripcion;
}
