package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.LoginRequestDTO;
import com.lubcker.ecommerce.dto.LoginResponseDTO;
import com.lubcker.ecommerce.dto.RegistroRequestDTO;
import com.lubcker.ecommerce.model.Cliente;
import com.lubcker.ecommerce.model.Negocio;
import com.lubcker.ecommerce.model.Rubros;
import com.lubcker.ecommerce.repository.NegocioRepository;
import com.lubcker.ecommerce.model.Usuario;
import com.lubcker.ecommerce.repository.ClienteRepository;
import com.lubcker.ecommerce.repository.UsuarioRepository;
import com.lubcker.ecommerce.security.ContrasenaService;
import com.lubcker.ecommerce.security.JwtService;
import com.lubcker.ecommerce.security.LoginIntentosService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Login y registro. Son las unicas rutas de escritura que no piden sesion.
@Tag(name = "Autenticación", description = "Inicio de sesión (token JWT) y registro de clientes con los datos de su negocio.")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ContrasenaService contrasenaService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LoginIntentosService loginIntentos;

    @Autowired
    private NegocioRepository negocioRepository;

    @Operation(summary = "Iniciar sesión y obtener el token JWT (5 intentos fallidos bloquean la cuenta 15 minutos)", description = "Acceso: **Público** (no requiere sesión). Copiar el `token` de la respuesta y pegarlo en el botón Authorize.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Sesión iniciada",
                        content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
                @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos", content = @Content),
                @ApiResponse(responseCode = "403", description = "La cuenta está desactivada", content = @Content),
                @ApiResponse(responseCode = "429", description = "Demasiados intentos fallidos: cuenta bloqueada 15 minutos", content = @Content)})
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO datos) {
        String email = datos.getEmail() == null ? "" : datos.getEmail().trim();

        long minutos = loginIntentos.minutosBloqueado(email);
        if (minutos > 0) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("mensaje",
                    "Demasiados intentos fallidos. Probá de nuevo en " + minutos
                            + (minutos == 1 ? " minuto." : " minutos.")));
        }

        // Mismo error si el correo no existe o la contrasena es incorrecta (no revela cuentas)
        Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(u -> contrasenaService.verificar(u, datos.getContrasena()))
                .orElse(null);

        if (usuario == null) {
            loginIntentos.registrarFallo(email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Correo o contraseña incorrectos."));
        }

        loginIntentos.registrarExito(email);

        // Solo se avisa despues de validar la contraseña (no revela qué cuentas existen)
        if (!usuario.isActivo()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje",
                    "Tu cuenta está desactivada. Comunicate con la tienda para reactivarla."));
        }
        return ResponseEntity.ok(respuesta(usuario));
    }

    // Crea cliente + usuario + negocio en una sola transaccion (si falla algo no queda nada a medias)
    @Operation(summary = "Registrar un cliente junto con los datos de su negocio", description = "Acceso: **Público** (no requiere sesión). El rol siempre es CLIENTE; el rol ADMIN solo se asigna en la base.",
            responses = {
                @ApiResponse(responseCode = "201", description = "Cuenta creada (devuelve token y usuario)",
                        content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
                @ApiResponse(responseCode = "400", description = "Datos inválidos o rubro inexistente", content = @Content),
                @ApiResponse(responseCode = "409", description = "Ya existe una cuenta con ese correo", content = @Content)})
    @PostMapping("/registro")
    @Transactional
    public ResponseEntity<?> registro(@Valid @RequestBody RegistroRequestDTO datos) {
        String email = datos.getEmail().trim();

        if (!Rubros.esValido(datos.getNegocio().getRubro())) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El rubro elegido no es válido."));
        }

        if (usuarioRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "Ya existe una cuenta con ese correo electrónico."));
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(datos.getNombre().trim());
        cliente.setApellido(datos.getApellido().trim());
        cliente.setTelefono(datos.getTelefono().trim());
        cliente = clienteRepository.save(cliente);

        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setNombreUsuario(datos.getNombre().trim());
        usuario.setEmail(email);
        usuario.setContrasena(contrasenaService.hashear(datos.getContrasena()));
        usuario.setRol("CLIENTE"); // el rol ADMIN solo se asigna directamente en la base
        usuario = usuarioRepository.save(usuario);

        Negocio negocio = new Negocio();
        negocio.setCliente(cliente);
        NegocioController.aplicar(negocio, datos.getNegocio());
        negocioRepository.save(negocio);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta(usuario));
    }

    private LoginResponseDTO respuesta(Usuario u) {
        return new LoginResponseDTO(jwtService.generar(u.getIdUsuario()), UsuarioController.aDTO(u));
    }
}
