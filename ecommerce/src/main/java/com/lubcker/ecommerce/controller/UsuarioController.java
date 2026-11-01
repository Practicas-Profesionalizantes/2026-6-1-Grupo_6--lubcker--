package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.UsuarioDTO;
import com.lubcker.ecommerce.dto.UsuarioRequestDTO;
import com.lubcker.ecommerce.model.Cliente;
import com.lubcker.ecommerce.model.Usuario;
import com.lubcker.ecommerce.repository.ClienteRepository;
import com.lubcker.ecommerce.repository.UsuarioRepository;
import com.lubcker.ecommerce.security.ContrasenaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Usuarios", description = "Cuentas de usuario y perfil.")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ContrasenaService contrasenaService;

    // Nunca devolvemos la contrasena: UsuarioDTO no tiene ese campo a proposito.
    @Operation(summary = "Listar usuarios", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<UsuarioDTO> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioController::aDTO).collect(Collectors.toList());
    }

    @Operation(summary = "Obtener un usuario", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @GetMapping("/{id}")
    @PreAuthorize("@seguridad.puedeAccederUsuario(#id)")
    public ResponseEntity<UsuarioDTO> obtener(@PathVariable int id) {
        return usuarioRepository.findById(id)
                .map(u -> ResponseEntity.ok(aDTO(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear un usuario", description = "Acceso: **Solo Admin**.")
    @PostMapping
    public ResponseEntity<UsuarioDTO> crear(@RequestBody UsuarioRequestDTO datos) {
        if (datos.getIdCliente() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Si el email ya existe devolvemos 409 (Conflict): el login busca por email,
        // asi que dos usuarios con el mismo correo lo romperian.
        if (usuarioRepository.findByEmail(datos.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Cliente cliente = clienteRepository.findById(datos.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + datos.getIdCliente()));

        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setNombreUsuario(datos.getNombreUsuario());
        usuario.setEmail(datos.getEmail());
        usuario.setContrasena(contrasenaService.hashear(datos.getContrasena()));
        usuario.setRol("CLIENTE"); // el rol ADMIN solo se asigna directamente en la base

        Usuario guardado = usuarioRepository.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(aDTO(guardado));
    }

    // Edicion de perfil. Los campos null no se tocan. El rol NO se puede cambiar desde aqui.
    @Operation(summary = "Editar un usuario (perfil y datos del negocio)", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PutMapping("/{id}")
    @PreAuthorize("@seguridad.puedeAccederUsuario(#id)")
    public ResponseEntity<?> actualizar(@PathVariable int id, @RequestBody UsuarioRequestDTO datos) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();

        if (datos.getEmail() != null && !datos.getEmail().equalsIgnoreCase(usuario.getEmail())) {
            var otro = usuarioRepository.findByEmail(datos.getEmail());
            if (otro.isPresent() && otro.get().getIdUsuario() != id) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            usuario.setEmail(datos.getEmail());
        }

        // La contraseña solo la cambia un administrador (PUT /api/admin/usuarios/{id}/contrasena)
        if (datos.getContrasena() != null && !datos.getContrasena().isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje",
                    "La contraseña solo la puede cambiar un administrador."));
        }

        if (datos.getNombreUsuario() != null && !datos.getNombreUsuario().isBlank()) {
            usuario.setNombreUsuario(datos.getNombreUsuario());
            // El nombre tambien vive en el cliente: se mantiene sincronizado.
            Cliente cliente = usuario.getCliente();
            if (cliente != null) {
                cliente.setNombre(datos.getNombreUsuario());
                if (datos.getApellido() != null) cliente.setApellido(datos.getApellido());
                clienteRepository.save(cliente);
            }
        }

        if (datos.getTelefono() != null && usuario.getCliente() != null) {
            String telefono = datos.getTelefono().trim();
            if (!telefono.matches("^[+0-9][0-9 ()-]{6,19}$")) {
                return ResponseEntity.badRequest().body(Map.of("mensaje",
                        "El teléfono no es válido. Ingresalo con el código de área, por ejemplo 11 4430-6194."));
            }
            usuario.getCliente().setTelefono(telefono);
            clienteRepository.save(usuario.getCliente());
        }

        if (datos.getFotoPerfil() != null) {
            usuario.setFotoPerfil(datos.getFotoPerfil().isBlank() ? null : datos.getFotoPerfil());
        }

        return ResponseEntity.ok(aDTO(usuarioRepository.save(usuario)));
    }

    public static UsuarioDTO aDTO(Usuario u) {
        return new UsuarioDTO(
                u.getIdUsuario(),
                u.getCliente() != null ? u.getCliente().getIdCliente() : 0,
                u.getNombreUsuario(),
                u.getEmail(),
                u.getRol() != null ? u.getRol() : "CLIENTE",
                u.getFotoPerfil()
        );
    }
}