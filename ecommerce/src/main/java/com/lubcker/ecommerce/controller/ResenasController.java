package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.ResenasDTO;
import com.lubcker.ecommerce.dto.ResenasRequestDTO;
import com.lubcker.ecommerce.model.Cliente;
import com.lubcker.ecommerce.model.Producto;
import com.lubcker.ecommerce.model.Resenas;
import com.lubcker.ecommerce.repository.ClienteRepository;
import com.lubcker.ecommerce.repository.Detalle_OrdenRepository;
import com.lubcker.ecommerce.repository.ProductoRepository;
import com.lubcker.ecommerce.repository.ResenasRepository;
import com.lubcker.ecommerce.security.SeguridadService;
import com.lubcker.ecommerce.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Reseñas: las lee cualquiera; las escribe solo quien compro el producto (una por producto).
@Tag(name = "Reseñas", description = "Opiniones de clientes que compraron el producto (una por cliente y producto).")
@RestController
@RequestMapping("/api/resenas")
public class ResenasController {

    @Autowired
    private ResenasRepository resenasRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private Detalle_OrdenRepository detalleOrdenRepository;

    @Autowired
    private SeguridadService seguridad;

    @Operation(summary = "Listar todas las reseñas", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<ResenasDTO> listar() {
        return resenasRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    // Reseñas destacadas para la pagina principal (4 o 5 estrellas, las mas nuevas)
    @Operation(summary = "Listar las 6 reseñas más recientes de 4 o 5 estrellas", description = "Acceso: **Público** (no requiere sesión).")
    @GetMapping("/destacadas")
    public List<ResenasDTO> destacadas() {
        return resenasRepository.findTop6ByValoracionGreaterThanEqualOrderByIdResenasDesc(4)
                .stream()
                .filter(r -> r.getProducto() != null && r.getProducto().isActivo())
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Listar las reseñas de un producto", description = "Acceso: **Público** (no requiere sesión).")
    @GetMapping("/producto/{idProducto}")
    public List<ResenasDTO> listarPorProducto(@PathVariable int idProducto) {
        return resenasRepository.findByProductoIdProductoOrderByIdResenasDesc(idProducto)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Reseñas del usuario con sesion (el historial las usa para mostrar "Editar reseña")
    @Operation(summary = "Listar las reseñas del usuario con sesión", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @GetMapping("/mias")
    public List<ResenasDTO> mias() {
        UsuarioAutenticado u = seguridad.actual().orElseThrow();
        return resenasRepository.findByClienteIdCliente(u.idCliente())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ¿Puede el usuario reseñar este producto? -> { "comprado": true, "resena": {...} | null }
    @Operation(summary = "Saber si el usuario compró el producto y si ya lo reseñó", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @GetMapping("/estado/{idProducto}")
    public Map<String, Object> estado(@PathVariable int idProducto) {
        UsuarioAutenticado u = seguridad.actual().orElseThrow();
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("comprado", detalleOrdenRepository.clienteComproProducto(u.idCliente(), idProducto));
        respuesta.put("resena", resenasRepository
                .findByClienteIdClienteAndProductoIdProducto(u.idCliente(), idProducto)
                .map(this::toDTO)
                .orElse(null));
        return respuesta;
    }

    @Operation(summary = "Crear una reseña (solo de productos comprados, una por producto)", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody ResenasRequestDTO datos) {
        UsuarioAutenticado u = seguridad.actual().orElseThrow();
        int idCliente = u.idCliente();

        Producto producto = productoRepository.findById(datos.getIdProducto()).orElse(null);
        if (producto == null) return ResponseEntity.notFound().build();

        if (!detalleOrdenRepository.clienteComproProducto(idCliente, producto.getIdProducto())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Solo podés reseñar productos que compraste."));
        }
        if (resenasRepository.findByClienteIdClienteAndProductoIdProducto(idCliente, producto.getIdProducto()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "Ya dejaste una reseña de este producto. Podés editarla."));
        }

        Cliente cliente = clienteRepository.findById(idCliente).orElseThrow();

        Resenas resena = new Resenas();
        resena.setCliente(cliente);
        resena.setProducto(producto);
        resena.setValoracion(datos.getValoracion());
        resena.setDescripcion(limpiar(datos.getDescripcion()));

        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(resenasRepository.save(resena)));
    }

    @Operation(summary = "Editar una reseña propia", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @Valid @RequestBody ResenasRequestDTO datos) {
        return resenasRepository.findById(id)
                .filter(this::esPropia)
                .<ResponseEntity<?>>map(resena -> {
                    resena.setValoracion(datos.getValoracion());
                    resena.setDescripcion(limpiar(datos.getDescripcion()));
                    return ResponseEntity.ok(toDTO(resenasRepository.save(resena)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar una reseña propia", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        Resenas resena = resenasRepository.findById(id).filter(this::esPropia).orElse(null);
        if (resena == null) return ResponseEntity.notFound().build();
        resenasRepository.delete(resena);
        return ResponseEntity.noContent().build();
    }

    // Una reseña solo la edita/borra su autor (o un admin). Si no es suya se responde 404.
    private boolean esPropia(Resenas r) {
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

    private ResenasDTO toDTO(Resenas r) {
        Producto p = r.getProducto();
        return new ResenasDTO(
                r.getIdResenas(),
                r.getCliente() != null ? r.getCliente().getIdCliente() : 0,
                nombrePublico(r.getCliente()),
                p != null ? p.getIdProducto() : 0,
                p != null ? p.getNombre() : null,
                p != null ? p.getImagen() : null,
                r.getValoracion(),
                r.getDescripcion()
        );
    }
}
