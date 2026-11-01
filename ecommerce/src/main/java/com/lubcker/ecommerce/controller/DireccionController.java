package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.DireccionDTO;
import com.lubcker.ecommerce.dto.DireccionRequestDTO;
import com.lubcker.ecommerce.model.Cliente;
import com.lubcker.ecommerce.model.Direccion;
import com.lubcker.ecommerce.repository.ClienteRepository;
import com.lubcker.ecommerce.repository.DireccionRepository;
import com.lubcker.ecommerce.security.SeguridadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Direcciones", description = "Direcciones de envío de los clientes.")
@RestController
@RequestMapping("/api/direcciones")
public class DireccionController {

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private SeguridadService seguridad;

    @Operation(summary = "Listar todas las direcciones", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<DireccionDTO> listar() {
        return direccionRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Operation(summary = "Obtener una dirección", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @GetMapping("/{id}")
    public ResponseEntity<DireccionDTO> obtener(@PathVariable int id) {
        return direccionRepository.findById(id)
                .filter(this::esPropia)
                .map(d -> ResponseEntity.ok(toDTO(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/direcciones/cliente/{idCliente} -> direcciones guardadas de un cliente
    @Operation(summary = "Listar las direcciones de un cliente", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @GetMapping("/cliente/{idCliente}")
    @PreAuthorize("@seguridad.puedeAccederCliente(#idCliente)")
    public List<DireccionDTO> listarPorCliente(@PathVariable int idCliente) {
        return direccionRepository.findAll().stream()
                .filter(d -> d.getCliente() != null && d.getCliente().getIdCliente() == idCliente)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Crear una dirección", description = "Acceso: **Solo Admin**.")
    @PostMapping
    public ResponseEntity<DireccionDTO> crear(@Valid @RequestBody DireccionRequestDTO datos) {
        if (!seguridad.puedeAccederCliente(datos.getIdCliente())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Cliente cliente = clienteRepository.findById(datos.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + datos.getIdCliente()));

        Direccion direccion = new Direccion();
        direccion.setCliente(cliente);
        direccion.setProvincia(datos.getProvincia());
        direccion.setCiudad(datos.getCiudad());
        direccion.setCalle(datos.getCalle());
        direccion.setAltura(datos.getAltura());

        Direccion guardada = direccionRepository.save(direccion);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardada));
    }

    @Operation(summary = "Editar una dirección", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PutMapping("/{id}")
    public ResponseEntity<DireccionDTO> actualizar(@PathVariable int id, @Valid @RequestBody DireccionRequestDTO datos) {
        return direccionRepository.findById(id)
                .filter(this::esPropia)
                .map(direccion -> {
                    direccion.setProvincia(datos.getProvincia());
                    direccion.setCiudad(datos.getCiudad());
                    direccion.setCalle(datos.getCalle());
                    direccion.setAltura(datos.getAltura());
                    return ResponseEntity.ok(toDTO(direccionRepository.save(direccion)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar una dirección", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        Direccion direccion = direccionRepository.findById(id).filter(this::esPropia).orElse(null);
        if (direccion == null) return ResponseEntity.notFound().build();
        direccionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Una direccion solo la ve/edita su dueño (o un admin). Si no es suya se responde 404.
    private boolean esPropia(Direccion d) {
        return d.getCliente() != null && seguridad.puedeAccederCliente(d.getCliente().getIdCliente());
    }

    private DireccionDTO toDTO(Direccion d) {
        return new DireccionDTO(
                d.getIdDireccion(),
                d.getCliente() != null ? d.getCliente().getIdCliente() : 0,
                d.getProvincia(),
                d.getCiudad(),
                d.getCalle(),
                d.getAltura()
        );
    }
}