package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnvioDTO {
    private int idEnvio;
    private int idOrden;
    private int idDireccion;
    private int idEmpresa;
    private String nombreEmpresa;
    private String estado;
    // Fecha estimada de entrega (yyyy-MM-dd), franja horaria y mensaje del dueño; null si todavia no se definio
    private String fechaEntrega;
    private String franjaHoraria;
    private String notaCliente;
}