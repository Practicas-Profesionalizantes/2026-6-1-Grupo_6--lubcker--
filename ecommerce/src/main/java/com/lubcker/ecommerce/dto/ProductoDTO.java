package com.lubcker.ecommerce.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDTO {
    private int idProducto;
    private String nombre;
    private int precio;
    private int idCategoria;
    private String nombreCategoria;
    private String imagen;
    private int stock;
    private boolean activo;
    private String unidadMedida;
    private List<Integer> presentaciones;
}