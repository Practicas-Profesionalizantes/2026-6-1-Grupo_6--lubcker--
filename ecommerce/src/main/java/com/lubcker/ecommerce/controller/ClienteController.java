package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.ClienteDTO;
import com.lubcker.ecommerce.dto.ClienteRequestDTO;
import com.lubcker.ecommerce.model.Cliente;
import com.lubcker.ecommerce.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Clientes", description = "Datos personales de los clientes.")
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Operation(summary = "Listar clientes", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<ClienteDTO> listar() {
        return clienteRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Obtener un cliente", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @GetMapping("/{id}")
    @PreAuthorize("@seguridad.puedeAccederCliente(#id)")
    public ResponseEntity<ClienteDTO> obtener(@PathVariable int id) {
        return clienteRepository.findById(id)
                .map(cliente -> ResponseEntity.ok(toDTO(cliente)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear un cliente", description = "Acceso: **Solo Admin**.")
    @PostMapping
    public ResponseEntity<ClienteDTO> crear(@RequestBody ClienteRequestDTO datos) {
        Cliente cliente = new Cliente();
        cliente.setNombre(datos.getNombre());
        cliente.setApellido(datos.getApellido());

        Cliente guardado = clienteRepository.save(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardado));
    }

    @Operation(summary = "Editar un cliente", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PutMapping("/{id}")
    @PreAuthorize("@seguridad.puedeAccederCliente(#id)")
    public ResponseEntity<ClienteDTO> actualizar(@PathVariable int id, @RequestBody ClienteRequestDTO datos) {
        return clienteRepository.findById(id)
                .map(cliente -> {
                    cliente.setNombre(datos.getNombre());
                    cliente.setApellido(datos.getApellido());
                    Cliente actualizado = clienteRepository.save(cliente);
                    return ResponseEntity.ok(toDTO(actualizado));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar un cliente", description = "Acceso: **Solo Admin**.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        if (!clienteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        clienteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ClienteDTO toDTO(Cliente cliente) {
        return new ClienteDTO(
                cliente.getIdCliente(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getTelefono()
        );
    }
}