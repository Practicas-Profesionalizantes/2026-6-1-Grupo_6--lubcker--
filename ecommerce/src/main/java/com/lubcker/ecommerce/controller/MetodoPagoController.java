package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.MetodoPagoDTO;
import com.lubcker.ecommerce.dto.MetodoPagoRequestDTO;
import com.lubcker.ecommerce.model.MetodoPago;
import com.lubcker.ecommerce.repository.MetodoPagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Métodos de pago", description = "Medios de pago disponibles.")
@RestController
@RequestMapping("/api/metodos-pago")
public class MetodoPagoController {

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    @Operation(summary = "Listar métodos de pago", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<MetodoPagoDTO> listar() {
        return metodoPagoRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Operation(summary = "Obtener un método de pago", description = "Acceso: **Público** (no requiere sesión).")
    @GetMapping("/{id}")
    public ResponseEntity<MetodoPagoDTO> obtener(@PathVariable int id) {
        return metodoPagoRepository.findById(id)
                .map(m -> ResponseEntity.ok(toDTO(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear un método de pago", description = "Acceso: **Solo Admin**.")
    @PostMapping
    public ResponseEntity<MetodoPagoDTO> crear(@RequestBody MetodoPagoRequestDTO datos) {
        MetodoPago metodo = new MetodoPago();
        metodo.setNombre(datos.getNombre());
        MetodoPago guardado = metodoPagoRepository.save(metodo);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardado));
    }

    @Operation(summary = "Editar un método de pago", description = "Acceso: **Solo Admin**.")
    @PutMapping("/{id}")
    public ResponseEntity<MetodoPagoDTO> actualizar(@PathVariable int id, @RequestBody MetodoPagoRequestDTO datos) {
        return metodoPagoRepository.findById(id)
                .map(metodo -> {
                    metodo.setNombre(datos.getNombre());
                    return ResponseEntity.ok(toDTO(metodoPagoRepository.save(metodo)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar un método de pago", description = "Acceso: **Solo Admin**.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        if (!metodoPagoRepository.existsById(id)) return ResponseEntity.notFound().build();
        metodoPagoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private MetodoPagoDTO toDTO(MetodoPago m) {
        return new MetodoPagoDTO(m.getIdMetodo(), m.getNombre());
    }
}