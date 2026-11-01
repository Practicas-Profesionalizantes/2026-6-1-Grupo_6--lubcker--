package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.EnvioDTO;
import com.lubcker.ecommerce.dto.EntregaRequestDTO;
import com.lubcker.ecommerce.dto.EnvioRequestDTO;
import jakarta.validation.Valid;
import com.lubcker.ecommerce.mercadopago.OrdenCancelacionService;
import com.lubcker.ecommerce.model.Direccion;
import com.lubcker.ecommerce.model.Empresa_Logistica;
import com.lubcker.ecommerce.model.Envio;
import com.lubcker.ecommerce.model.Orden;
import com.lubcker.ecommerce.model.Detalle_Orden;
import com.lubcker.ecommerce.model.Producto;
import com.lubcker.ecommerce.repository.Detalle_OrdenRepository;
import com.lubcker.ecommerce.repository.DireccionRepository;
import com.lubcker.ecommerce.repository.Empresa_LogisticaRepository;
import com.lubcker.ecommerce.repository.EnvioRepository;
import com.lubcker.ecommerce.repository.OrdenRepository;
import com.lubcker.ecommerce.repository.ProductoRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Envíos", description = "Seguimiento y cambio de estado de los envíos. Cancelar un envío devuelve el stock.")
@RestController
@RequestMapping("/api/envios")
public class EnvioController {

    @Autowired
    private EnvioRepository envioRepository;

    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired
    private Detalle_OrdenRepository detalleOrdenRepository;

    @Autowired
    private OrdenCancelacionService cancelacionService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private Empresa_LogisticaRepository empresaLogisticaRepository;

    @Operation(summary = "Listar todos los envíos", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<EnvioDTO> listar() {
        return envioRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Operation(summary = "Obtener un envío", description = "Acceso: **Solo Admin**.")
    @GetMapping("/{id}")
    public ResponseEntity<EnvioDTO> obtener(@PathVariable int id) {
        return envioRepository.findById(id)
                .map(e -> ResponseEntity.ok(toDTO(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/envios/orden/{idOrden} -> seguimiento del envio de una orden puntual
    @Operation(summary = "Obtener el envío de un pedido", description = "Acceso: **Solo Admin**.")
    @GetMapping("/orden/{idOrden}")
    public ResponseEntity<EnvioDTO> obtenerPorOrden(@PathVariable int idOrden) {
        return envioRepository.findAll().stream()
                .filter(e -> e.getOrden() != null && e.getOrden().getIdOrden() == idOrden)
                .findFirst()
                .map(e -> ResponseEntity.ok(toDTO(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/envios/cliente/{idCliente} -> envios de todas las ordenes de un cliente (para su historial)
    @Operation(summary = "Listar los envíos de un cliente", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @GetMapping("/cliente/{idCliente}")
    @PreAuthorize("@seguridad.puedeAccederCliente(#idCliente)")
    public List<EnvioDTO> listarPorCliente(@PathVariable int idCliente) {
        return envioRepository.findAll().stream()
                .filter(e -> e.getOrden() != null && e.getOrden().getCliente() != null
                        && e.getOrden().getCliente().getIdCliente() == idCliente)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Crear un envío", description = "Acceso: **Solo Admin**.")
    @PostMapping
    public ResponseEntity<EnvioDTO> crear(@RequestBody EnvioRequestDTO datos) {
        Orden orden = ordenRepository.findById(datos.getIdOrden())
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + datos.getIdOrden()));
        Direccion direccion = direccionRepository.findById(datos.getIdDireccion())
                .orElseThrow(() -> new RuntimeException("Direccion no encontrada: " + datos.getIdDireccion()));
        Empresa_Logistica empresa = empresaLogisticaRepository.findById(datos.getIdEmpresa())
                .orElseThrow(() -> new RuntimeException("Empresa logistica no encontrada: " + datos.getIdEmpresa()));

        Envio envio = new Envio();
        envio.setOrden(orden);
        envio.setDireccion(direccion);
        envio.setEmpresaLogistica(empresa);
        envio.setEstado(datos.getEstado() != null ? datos.getEstado() : "pendiente");

        Envio guardado = envioRepository.save(envio);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardado));
    }

    // PUT /api/envios/{id}/estado -> el uso mas comun: actualizar el estado del envio
    @Operation(summary = "Cambiar el estado del envío (el cuerpo es el texto del estado; un pedido cancelado no se reactiva)", description = "Acceso: **Solo Admin**.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "text/plain",
                    schema = @Schema(type = "string", allowableValues = {"Pendiente", "En preparación", "En camino", "Entregado", "Cancelado"}, example = "En camino"))))
    @PutMapping("/{id}/estado")
    // Al cancelar, las unidades vuelven al stock. Un pedido cancelado no se puede reactivar
    // (el stock ya se devolvio y quizas se vendio a otro cliente).
    @Transactional
    public ResponseEntity<?> actualizarEstado(@PathVariable int id, @RequestBody String estado) {
        Envio envio = envioRepository.findById(id).orElse(null);
        if (envio == null) return ResponseEntity.notFound().build();

        String nuevo = estado == null ? "" : estado.trim();
        if (nuevo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El estado no puede estar vacío."));
        }

        boolean yaCancelado = "cancelado".equalsIgnoreCase(envio.getEstado());
        boolean cancelar = "cancelado".equalsIgnoreCase(nuevo);

        if (yaCancelado && !cancelar) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "Un pedido cancelado no se puede reactivar."));
        }

        if (cancelar && !yaCancelado && envio.getOrden() != null) {
            cancelacionService.devolverStock(envio.getOrden().getIdOrden());
        }

        envio.setEstado(nuevo);
        return ResponseEntity.ok(toDTO(envioRepository.save(envio)));
    }

    // Fecha estimada, franja horaria y mensaje del dueño para el cliente. Se ve en el historial de compras.
    @Operation(summary = "Definir la entrega: fecha estimada, franja horaria y mensaje para el cliente", description = "Acceso: **Solo Admin**. La fecha no puede ser anterior a hoy (salvo que ya fuera esa) y un pedido cancelado no se puede programar.")
    @PutMapping("/{id}/entrega")
    public ResponseEntity<?> programarEntrega(@PathVariable int id, @Valid @RequestBody EntregaRequestDTO datos) {
        Envio envio = envioRepository.findById(id).orElse(null);
        if (envio == null) return ResponseEntity.notFound().build();

        if ("cancelado".equalsIgnoreCase(envio.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "Un pedido cancelado no se puede programar."));
        }

        java.time.LocalDate fecha = null;
        String texto = datos.getFechaEntrega() == null ? "" : datos.getFechaEntrega().trim();
        if (!texto.isEmpty()) {
            try {
                fecha = java.time.LocalDate.parse(texto);
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "La fecha no es válida."));
            }
            boolean esLaMisma = fecha.equals(envio.getFechaEntrega());
            if (fecha.isBefore(java.time.LocalDate.now()) && !esLaMisma) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "La fecha de entrega no puede ser anterior a hoy."));
            }
        }

        envio.setFechaEntrega(fecha);
        envio.setFranjaHoraria(limpiar(datos.getFranjaHoraria()));
        envio.setNotaCliente(limpiar(datos.getNotaCliente()));
        return ResponseEntity.ok(toDTO(envioRepository.save(envio)));
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    @Operation(summary = "Eliminar un envío", description = "Acceso: **Solo Admin**.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        if (!envioRepository.existsById(id)) return ResponseEntity.notFound().build();
        envioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private EnvioDTO toDTO(Envio e) {
        return new EnvioDTO(
                e.getIdEnvio(),
                e.getOrden() != null ? e.getOrden().getIdOrden() : 0,
                e.getDireccion() != null ? e.getDireccion().getIdDireccion() : 0,
                e.getEmpresaLogistica() != null ? e.getEmpresaLogistica().getIdEmpresa() : 0,
                e.getEmpresaLogistica() != null ? e.getEmpresaLogistica().getNombre() : null,
                e.getEstado(),
                e.getFechaEntrega() != null ? e.getFechaEntrega().toString() : null,
                e.getFranjaHoraria(),
                e.getNotaCliente()
        );
    }
}