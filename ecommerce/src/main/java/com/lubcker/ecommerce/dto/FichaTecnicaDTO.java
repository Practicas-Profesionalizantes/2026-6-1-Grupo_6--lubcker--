package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FichaTecnicaDTO {
    private int idFicha;
    private int idProducto;
    private String modelo;
    private String presentacion;
    private String descripcion;
}