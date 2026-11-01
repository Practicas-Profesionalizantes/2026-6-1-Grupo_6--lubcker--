package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequestDTO {
    // Integer (no int): en las ediciones de perfil este campo no viene en el JSON
    private Integer idCliente;
    private String nombreUsuario;
    private String email;
    private String contrasena;
    // null = no tocar; "" = quitar la foto
    private String fotoPerfil;
    // Para actualizar tambien el apellido del cliente asociado (opcional)
    private String apellido;
    // null = no tocar
    private String telefono;
}