package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.NegocioDTO;
import com.lubcker.ecommerce.model.Cliente;
import com.lubcker.ecommerce.model.Negocio;
import com.lubcker.ecommerce.model.Rubros;
import com.lubcker.ecommerce.repository.ClienteRepository;
import com.lubcker.ecommerce.repository.NegocioRepository;
import com.lubcker.ecommerce.security.SeguridadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Datos del negocio del usuario con sesion.
@Tag(name = "Negocios", description = "Datos del negocio de cada cliente y lista de rubros.")
@RestController
@RequestMapping("/api/negocios")
public class NegocioController {

    @Autowired
    private NegocioRepository negocioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private SeguridadService seguridad;

    // Publico: el formulario de registro lo usa para armar la lista de rubros
    @Operation(summary = "Listar los rubros disponibles para registrarse", description = "Acceso: **Público** (no requiere sesión).")
    @GetMapping("/rubros")
    public List<String> rubros() {
        return Rubros.TODOS;
    }

    // GET /api/negocios/mio -> 204 si todavia no cargo los datos del negocio
    @Operation(summary = "Obtener el negocio del usuario con sesión", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @GetMapping("/mio")
    public ResponseEntity<NegocioDTO> mio() {
        return negocioRepository.findByClienteIdCliente(idCliente())
                .map(n -> ResponseEntity.ok(aDTO(n)))
                .orElse(ResponseEntity.noContent().build());
    }

    // PUT /api/negocios/mio -> crea o actualiza los datos del negocio
    @Operation(summary = "Editar el negocio del usuario con sesión", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PutMapping("/mio")
    public ResponseEntity<?> guardar(@Valid @RequestBody NegocioDTO datos) {
        if (!Rubros.esValido(datos.getRubro())) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El rubro elegido no es válido."));
        }
        int idCliente = idCliente();
        Negocio negocio = negocioRepository.findByClienteIdCliente(idCliente).orElseGet(() -> {
            Negocio nuevo = new Negocio();
            Cliente cliente = clienteRepository.findById(idCliente).orElseThrow();
            nuevo.setCliente(cliente);
            return nuevo;
        });
        aplicar(negocio, datos);
        return ResponseEntity.ok(aDTO(negocioRepository.save(negocio)));
    }

    public static void aplicar(Negocio negocio, NegocioDTO datos) {
        negocio.setNombre(datos.getNombre().trim());
        negocio.setRubro(datos.getRubro());
        negocio.setProvincia(datos.getProvincia().trim());
        negocio.setCiudad(datos.getCiudad().trim());
        negocio.setCalle(datos.getCalle().trim());
        negocio.setAltura(datos.getAltura());
    }

    public static NegocioDTO aDTO(Negocio n) {
        return new NegocioDTO(n.getNombre(), n.getRubro(), n.getProvincia(), n.getCiudad(), n.getCalle(), n.getAltura());
    }

    private int idCliente() {
        return seguridad.actual().orElseThrow().idCliente();
    }
}
