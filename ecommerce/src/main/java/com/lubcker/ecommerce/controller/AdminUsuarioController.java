package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.AdminUsuarioDTO;
import com.lubcker.ecommerce.dto.CambioContrasenaDTO;
import com.lubcker.ecommerce.dto.DireccionDTO;
import com.lubcker.ecommerce.model.Cliente;
import com.lubcker.ecommerce.model.Usuario;
import com.lubcker.ecommerce.repository.*;
import com.lubcker.ecommerce.service.OrdenEliminacionService;
import com.lubcker.ecommerce.security.ContrasenaService;
import com.lubcker.ecommerce.security.LoginIntentosService;
import com.lubcker.ecommerce.security.SeguridadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

// "Gestionar usuarios" del panel de admin. Todas las rutas /api/admin/** son solo para ADMIN
// (regla general de SecurityConfig).
@Tag(name = "Usuarios (admin)", description = "Gestión de cuentas por el administrador: listar, cambiar contraseña, activar/desactivar y eliminar.")
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private DireccionRepository direccionRepository;
    @Autowired private OrdenRepository ordenRepository;
    @Autowired private OrdenEliminacionService eliminacionPedidos;
    @Autowired private Detalle_OrdenRepository detalleOrdenRepository;
    @Autowired private ResenasRepository resenasRepository;
    @Autowired private ResenaGeneralRepository resenaGeneralRepository;
    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private ContrasenaService contrasenaService;
    @Autowired private LoginIntentosService loginIntentos;
    @Autowired private SeguridadService seguridad;

    @Operation(summary = "Listar todos los usuarios con negocio, direcciones, pedidos y total gastado", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<AdminUsuarioDTO> listar() {
        return usuarioRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Usuario::getIdUsuario).reversed())
                .map(this::aDTO)
                .toList();
    }

    // El admin pone una contraseña nueva sin necesitar la actual (ej: el cliente se la olvidó)
    @Operation(summary = "Cambiar la contraseña de un usuario sin pedir la actual", description = "Acceso: **Solo Admin**.")
    @PutMapping("/{id}/contrasena")
    public ResponseEntity<?> cambiarContrasena(@PathVariable int id, @Valid @RequestBody CambioContrasenaDTO datos) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();

        usuario.setContrasena(contrasenaService.hashear(datos.getNueva()));
        usuarioRepository.save(usuario);
        loginIntentos.registrarExito(usuario.getEmail()); // si estaba bloqueado por intentos, se libera
        return ResponseEntity.noContent().build();
    }

    // PUT /api/admin/usuarios/{id}/activo  body: {"activo": false}
    @Operation(summary = "Activar o desactivar una cuenta", description = "Acceso: **Solo Admin**.")
    @PutMapping("/{id}/activo")
    public ResponseEntity<?> cambiarActivo(@PathVariable int id, @RequestBody Map<String, Boolean> cuerpo) {
        Boolean activo = cuerpo.get("activo");
        if (activo == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Falta el campo \"activo\"."));
        }
        if (esUnoMismo(id)) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "No podés desactivar tu propia cuenta."));
        }
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();

        usuario.setActivo(activo);
        return ResponseEntity.ok(aDTO(usuarioRepository.save(usuario)));
    }

    // Solo se puede eliminar un usuario sin pedidos: los pedidos son registros de ventas.
    // Si tiene pedidos, se desactiva.
    @Operation(summary = "Eliminar un usuario (solo si no tiene pedidos)", description = "Acceso: **Solo Admin**.")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable int id) {
        if (esUnoMismo(id)) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "No podés eliminar tu propia cuenta."));
        }
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();

        Cliente cliente = usuario.getCliente();
        int idCliente = cliente != null ? cliente.getIdCliente() : 0;

        if (idCliente != 0 && ordenRepository.contarPagados(idCliente) > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",
                    "Este usuario tiene pedidos registrados y no se puede eliminar. Desactivalo para que no pueda ingresar."));
        }

        // Los pedidos sin pagar no son compras: se borran con el usuario (las unidades vuelven al stock)
        if (idCliente != 0) {
            ordenRepository.idsSinPagar(idCliente).forEach(eliminacionPedidos::eliminar);
        }

        // Borrados directos (sin cargar entidades) para evitar el problema de Hibernate
        // con la relacion Cliente <-> Usuario
        usuarioRepository.borrarPorId(id);
        if (idCliente != 0) {
            favoritoRepository.borrarPorCliente(idCliente);
            resenasRepository.borrarPorCliente(idCliente);
            resenaGeneralRepository.borrarPorCliente(idCliente);
            direccionRepository.borrarPorCliente(idCliente);
            negocioRepository.borrarPorCliente(idCliente);
            clienteRepository.borrarPorId(idCliente);
        }
        return ResponseEntity.noContent().build();
    }

    private boolean esUnoMismo(int idUsuario) {
        return seguridad.actual().map(u -> u.idUsuario() == idUsuario).orElse(false);
    }

    private AdminUsuarioDTO aDTO(Usuario u) {
        Cliente c = u.getCliente();
        int idCliente = c != null ? c.getIdCliente() : 0;

        List<DireccionDTO> direcciones = direccionRepository.findByClienteIdCliente(idCliente).stream()
                .map(d -> new DireccionDTO(d.getIdDireccion(), idCliente, d.getProvincia(), d.getCiudad(),
                        d.getCalle(), d.getAltura()))
                .toList();

        return new AdminUsuarioDTO(
                u.getIdUsuario(),
                idCliente,
                c != null ? c.getNombre() : u.getNombreUsuario(),
                c != null ? c.getApellido() : null,
                u.getEmail(),
                u.getRol() != null ? u.getRol() : "CLIENTE",
                u.isActivo(),
                u.getFotoPerfil(),
                negocioRepository.findByClienteIdCliente(idCliente).map(NegocioController::aDTO).orElse(null),
                direcciones,
                ordenRepository.contarPagados(idCliente),
                detalleOrdenRepository.totalGastado(idCliente)
        );
    }
}
