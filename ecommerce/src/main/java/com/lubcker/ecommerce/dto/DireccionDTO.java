package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DireccionDTO {
    private int idDireccion;
    private int idCliente;
    private String provincia;
    private String ciudad;
    private String calle;
    private int altura;
}