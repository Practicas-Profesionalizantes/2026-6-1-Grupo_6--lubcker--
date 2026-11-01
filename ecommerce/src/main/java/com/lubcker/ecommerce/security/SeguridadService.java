package com.lubcker.ecommerce.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

// Reglas de "este dato es mio". Se usa desde los controllers y desde @PreAuthorize:
//   @PreAuthorize("@seguridad.puedeAccederCliente(#idCliente)")
@Service("seguridad")
public class SeguridadService {

    public Optional<UsuarioAutenticado> actual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return Optional.of(usuario);
        }
        return Optional.empty();
    }

    public boolean esAdmin() {
        return actual().map(UsuarioAutenticado::esAdmin).orElse(false);
    }

    // El cliente puede ver/editar sus propios datos; el admin, los de cualquiera.
    public boolean puedeAccederCliente(int idCliente) {
        return actual().map(u -> u.esAdmin() || u.idCliente() == idCliente).orElse(false);
    }

    public boolean puedeAccederUsuario(int idUsuario) {
        return actual().map(u -> u.esAdmin() || u.idUsuario() == idUsuario).orElse(false);
    }
}
