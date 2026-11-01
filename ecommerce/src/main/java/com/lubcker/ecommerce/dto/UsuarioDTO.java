package com.lubcker.ecommerce.dto;

import lombok.*;

// OJO: a proposito NO incluye "contrasena". Nunca debe viajar en una respuesta.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    private int idUsuario;
    private int idCliente;
    private String nombreUsuario;
    private String email;
    private String rol;
    private String fotoPerfil;
}