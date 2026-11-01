package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.DetalleOrdenDTO;
import com.lubcker.ecommerce.dto.OrdenDTO;
import com.lubcker.ecommerce.dto.OrdenRequestDTO;
import com.lubcker.ecommerce.model.Cliente;
import com.lubcker.ecommerce.model.Detalle_Orden;
import com.lubcker.ecommerce.model.Orden;
import com.lubcker.ecommerce.repository.ClienteRepository;
import com.lubcker.ecommerce.repository.OrdenRepository;
import com.lubcker.ecommerce.service.OrdenEliminacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Órdenes", description = "Pedidos de los clientes.")
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OrdenEliminacionService eliminacion;

    @Operation(summary = "Listar todos los pedidos", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<OrdenDTO> listar() {
        return ordenRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Obtener un pedido", description = "Acceso: **Solo Admin**.")
    @GetMapping("/{id}")
    public ResponseEntity<OrdenDTO> obtener(@PathVariable int id) {
        return ordenRepository.findById(id)
                .map(orden -> ResponseEntity.ok(toDTO(orden)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Listar los pedidos de un cliente", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @GetMapping("/cliente/{idCliente}")
    @PreAuthorize("@seguridad.puedeAccederCliente(#idCliente)")
    public List<OrdenDTO> listarPorCliente(@PathVariable int idCliente) {
        return ordenRepository.findAll()
                .stream()
                .filter(o -> o.getCliente() != null && o.getCliente().getIdCliente() == idCliente)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // POST /api/ordenes -> crea la orden vacia (sin productos todavia).
    // Los productos se agregan despues via POST /api/detalles-orden
    @Operation(summary = "Crear un pedido", description = "Acceso: **Solo Admin**.")
    @PostMapping
    public ResponseEntity<OrdenDTO> crear(@RequestBody OrdenRequestDTO datos) {
        Cliente cliente = clienteRepository.findById(datos.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + datos.getIdCliente()));

        Orden orden = new Orden();
        orden.setCliente(cliente);
        orden.setFecha(new Date());

        Orden guardada = ordenRepository.save(orden);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardada));
    }

    @Operation(summary = "Eliminar un pedido con sus detalles, pagos y envío",
            description = "Acceso: **Solo Admin**. Si el pedido todavía no salió (envío Pendiente o En preparación), las unidades vuelven al stock.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        return eliminacion.eliminar(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private OrdenDTO toDTO(Orden orden) {
        List<DetalleOrdenDTO> detalles = orden.getDetalles() == null
                ? Collections.emptyList()
                : orden.getDetalles().stream()
                  .map(this::detalleToDTO)
                  .collect(Collectors.toList());

        return new OrdenDTO(
                orden.getIdOrden(),
                orden.getCliente() != null ? orden.getCliente().getIdCliente() : 0,
                orden.getCliente() != null ? orden.getCliente().getNombre() + " " + orden.getCliente().getApellido() : null,
                orden.getCliente() != null ? orden.getCliente().getTelefono() : null,
                orden.getFecha(),
                detalles
        );
    }

    private DetalleOrdenDTO detalleToDTO(Detalle_Orden d) {
        return new DetalleOrdenDTO(
                d.getIdDetalle(),
                d.getOrden().getIdOrden(),
                d.getProducto().getIdProducto(),
                d.getProducto().getNombre(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getSubtotal(),
                d.getTamano(),
                d.getProducto().getUnidadMedida()
        );
    }
}