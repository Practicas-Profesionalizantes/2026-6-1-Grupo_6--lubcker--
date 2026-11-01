package com.lubcker.ecommerce.dto;

import lombok.*;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrdenDTO {
    private int idOrden;
    private int idCliente;
    private String nombreCliente;
    // Para que el dueño pueda avisarle al cliente por WhatsApp
    private String telefonoCliente;
    private Date fecha;
    private List<DetalleOrdenDTO> detalles;
}