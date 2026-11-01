package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrdenRequestDTO {
    private int idCliente;
    // La fecha no se pide: el backend la setea con la fecha/hora actual al crear la orden
}