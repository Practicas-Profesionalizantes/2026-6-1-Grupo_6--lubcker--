package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.DetalleOrdenDTO;
import com.lubcker.ecommerce.dto.DetalleOrdenRequestDTO;
import com.lubcker.ecommerce.model.Detalle_Orden;
import com.lubcker.ecommerce.model.Orden;
import com.lubcker.ecommerce.model.Producto;
import com.lubcker.ecommerce.repository.Detalle_OrdenRepository;
import com.lubcker.ecommerce.repository.OrdenRepository;
import com.lubcker.ecommerce.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Detalles de orden", description = "Líneas de cada pedido (producto, cantidad, medida y precio).")
@RestController
@RequestMapping("/api/detalles-orden")
public class DetalleOrdenController {

    @Autowired
    private Detalle_OrdenRepository detalleOrdenRepository;

    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Operation(summary = "Listar detalles de orden", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<DetalleOrdenDTO> listar() {
        return detalleOrdenRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Obtener un detalle de orden", description = "Acceso: **Solo Admin**.")
    @GetMapping("/{id}")
    public ResponseEntity<DetalleOrdenDTO> obtener(@PathVariable int id) {
        return detalleOrdenRepository.findById(id)
                .map(d -> ResponseEntity.ok(toDTO(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear un detalle de orden", description = "Acceso: **Solo Admin**.")
    @PostMapping
    public ResponseEntity<DetalleOrdenDTO> crear(@RequestBody DetalleOrdenRequestDTO datos) {
        Orden orden = ordenRepository.findById(datos.getIdOrden())
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + datos.getIdOrden()));

        Producto producto = productoRepository.findById(datos.getIdProducto())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + datos.getIdProducto()));

        Detalle_Orden detalle = new Detalle_Orden();
        detalle.setOrden(orden);
        detalle.setProducto(producto);
        detalle.setCantidad(datos.getCantidad());
        detalle.setPrecioUnitario(datos.getPrecioUnitario());
        detalle.setSubtotal(datos.getCantidad() * datos.getPrecioUnitario()); // calculado, no lo manda el cliente

        Detalle_Orden guardado = detalleOrdenRepository.save(detalle);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardado));
    }

    @Operation(summary = "Editar un detalle de orden", description = "Acceso: **Solo Admin**.")
    @PutMapping("/{id}")
    public ResponseEntity<DetalleOrdenDTO> actualizar(@PathVariable int id, @RequestBody DetalleOrdenRequestDTO datos) {
        return detalleOrdenRepository.findById(id)
                .map(detalle -> {
                    detalle.setCantidad(datos.getCantidad());
                    detalle.setPrecioUnitario(datos.getPrecioUnitario());
                    detalle.setSubtotal(datos.getCantidad() * datos.getPrecioUnitario());
                    Detalle_Orden actualizado = detalleOrdenRepository.save(detalle);
                    return ResponseEntity.ok(toDTO(actualizado));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar un detalle de orden", description = "Acceso: **Solo Admin**.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        if (!detalleOrdenRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        detalleOrdenRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private DetalleOrdenDTO toDTO(Detalle_Orden d) {
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