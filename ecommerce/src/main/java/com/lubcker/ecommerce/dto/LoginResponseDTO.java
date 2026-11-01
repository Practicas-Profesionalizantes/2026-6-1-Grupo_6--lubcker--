package com.lubcker.ecommerce.dto;

import lombok.*;

// Respuesta de login y registro: el token para los proximos pedidos + los datos del usuario.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private UsuarioDTO usuario;
}
