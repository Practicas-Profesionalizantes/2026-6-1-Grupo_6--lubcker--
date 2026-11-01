package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnvioRequestDTO {
    private int idOrden;
    private int idDireccion;
    private int idEmpresa;
    private String estado;
}