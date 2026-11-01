package com.lubcker.ecommerce.security;

import com.lubcker.ecommerce.model.Usuario;
import com.lubcker.ecommerce.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Lee "Authorization: Bearer <token>". Si el token es valido y el usuario sigue existiendo,
// lo marca como autenticado. Un token invalido o vencido no da error aca: el pedido sigue
// como anonimo y las rutas protegidas responden 401.
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");

        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            jwtService.validar(cabecera.substring(7))
                    .flatMap(usuarioRepository::findById)
                    .filter(Usuario::isActivo) // un usuario desactivado pierde la sesion al instante
                    .ifPresent(this::autenticar);
        }

        chain.doFilter(request, response);
    }

    private void autenticar(Usuario usuario) {
        String rol = usuario.getRol() != null ? usuario.getRol() : "CLIENTE";
        int idCliente = usuario.getCliente() != null ? usuario.getCliente().getIdCliente() : 0;

        UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getIdUsuario(), idCliente, rol);
        var autenticacion = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
