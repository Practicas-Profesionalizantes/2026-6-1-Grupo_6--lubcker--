package com.lubcker.ecommerce.security;

// Datos del usuario que hizo el pedido, disponibles en toda la peticion.
public record UsuarioAutenticado(int idUsuario, int idCliente, String rol) {

    public boolean esAdmin() {
        return "ADMIN".equals(rol);
    }
}
