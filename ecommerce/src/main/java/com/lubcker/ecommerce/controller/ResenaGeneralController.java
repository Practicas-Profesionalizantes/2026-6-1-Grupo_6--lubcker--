package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.ResenaGeneralDTO;
import com.lubcker.ecommerce.dto.ResenaGeneralRequestDTO;
import com.lubcker.ecommerce.model.Cliente;
import com.lubcker.ecommerce.model.ResenaGeneral;
import com.lubcker.ecommerce.repository.ClienteRepository;
import com.lubcker.ecommerce.repository.ResenaGeneralRepository;
import com.lubcker.ecommerce.security.SeguridadService;
import com.lubcker.ecommerce.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Opiniones sobre Lubcker en general: las lee cualquiera; las escribe cualquier cliente con
// sesion (una por cliente, editable). No exige haber comprado: es una opinion de la tienda,
// no de un producto (las de producto estan en /api/resenas y si exigen compra).
@Tag(name = "Reseñas de Lubcker", description = "Opiniones generales sobre la tienda (una por cliente).")
@RestController
@RequestMapping("/api/resenas-generales")
public class ResenaGeneralController {

    @Autowired
    private ResenaGeneralRepository resenaGeneralRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private SeguridadService seguridad;

    @Operation(summary = "Listar las opiniones sobre Lubcker", description = "Acceso: **Público** (no requiere sesión).")
    @GetMapping
    public List<ResenaGeneralDTO> listar() {
        return resenaGeneralRepository.findAllByOrderByIdResenaGeneralDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Obtener la opinión propia sobre Lubcker (404 si todavía no dejó una)", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @GetMapping("/mia")
    public ResponseEntity<ResenaGeneralDTO> mia() {
        UsuarioAutenticado u = seguridad.actual().orElseThrow();
        return resenaGeneralRepository.findByClienteIdCliente(u.idCliente())
                .map(r -> ResponseEntity.ok(toDTO(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear la opinión sobre Lubcker (una por cliente)", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody ResenaGeneralRequestDTO datos) {
        UsuarioAutenticado u = seguridad.actual().orElseThrow();
        Cliente cliente = u.idCliente() != 0 ? clienteRepository.findById(u.idCliente()).orElse(null) : null;
        if (cliente == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Solo los clientes pueden dejar una opinión."));
        }
        if (resenaGeneralRepository.findByClienteIdCliente(cliente.getIdCliente()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "Ya dejaste tu opinión sobre Lubcker. Podés editarla."));
        }

        ResenaGeneral resena = new ResenaGeneral();
        resena.setCliente(cliente);
        resena.setValoracion(datos.getValoracion());
        resena.setDescripcion(limpiar(datos.getDescripcion()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(resenaGeneralRepository.save(resena)));
    }

    @Operation(summary = "Editar la opinión propia sobre Lubcker", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @Valid @RequestBody ResenaGeneralRequestDTO datos) {
        return resenaGeneralRepository.findById(id)
                .filter(this::esPropia)
                .<ResponseEntity<?>>map(resena -> {
                    resena.setValoracion(datos.getValoracion());
                    resena.setDescripcion(limpiar(datos.getDescripcion()));
                    return ResponseEntity.ok(toDTO(resenaGeneralRepository.save(resena)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar la opinión propia sobre Lubcker", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        ResenaGeneral resena = resenaGeneralRepository.findById(id).filter(this::esPropia).orElse(null);
        if (resena == null) return ResponseEntity.notFound().build();
        resenaGeneralRepository.delete(resena);
        return ResponseEntity.noContent().build();
    }

    // Solo la edita/borra su autor (o un admin). Si no es suya se responde 404.
    private boolean esPropia(ResenaGeneral r) {
        return r.getCliente() != null && seguridad.puedeAccederCliente(r.getCliente().getIdCliente());
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private String nombrePublico(Cliente c) {
        if (c == null) return "Cliente";
        String nombre = c.getNombre() != null ? c.getNombre() : "";
        String apellido = c.getApellido() != null && !c.getApellido().isBlank()
                ? " " + c.getApellido().trim().charAt(0) + "." : "";
        return (nombre + apellido).trim();
    }

    private ResenaGeneralDTO toDTO(ResenaGeneral r) {
        return new ResenaGeneralDTO(
                r.getIdResenaGeneral(),
                r.getCliente() != null ? r.getCliente().getIdCliente() : 0,
                nombrePublico(r.getCliente()),
                r.getValoracion(),
                r.getDescripcion()
        );
    }
}
