package com.lubcker.ecommerce.dto;

import lombok.*;
import java.util.List;

// Lo que manda el frontend al confirmar la compra.
// OJO: no incluye precios; los precios los toma el backend de la base de datos.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequestDTO {
    private int idCliente;
    private int idMetodoPago;
    private String provincia;
    private String ciudad;
    private String calle;
    private int altura;
    // Telefono de contacto para coordinar la entrega (se guarda en el cliente)
    private String telefono;
    private List<ItemCheckoutDTO> items;
}