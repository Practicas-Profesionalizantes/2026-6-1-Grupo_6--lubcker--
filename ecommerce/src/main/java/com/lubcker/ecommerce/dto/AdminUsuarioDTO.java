package com.lubcker.ecommerce.dto;

import lombok.*;

import java.util.List;

// Todo lo que el admin ve de un usuario en "Gestionar usuarios". Nunca incluye la contraseña.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUsuarioDTO {
    private int idUsuario;
    private int idCliente;
    private String nombre;
    private String apellido;
    private String email;
    private String rol;
    private boolean activo;
    private String fotoPerfil;
    private NegocioDTO negocio;          // null si no cargó los datos del negocio
    private List<DireccionDTO> direcciones;
    private long cantidadPedidos;
    private long totalGastado;           // solo pedidos pagados y sin cancelar
}
