package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.CheckoutRequestDTO;
import com.lubcker.ecommerce.dto.CheckoutResponseDTO;
import com.lubcker.ecommerce.service.CheckoutException;
import com.lubcker.ecommerce.service.CheckoutService;
import com.lubcker.ecommerce.security.SeguridadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Compra", description = "Confirmación de la compra: crea orden, detalles, pago y envío en una sola transacción.")
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private SeguridadService seguridad;

    // POST /api/checkout -> crea orden + detalles + pago + envio en una sola operacion
    @Operation(summary = "Confirmar la compra y obtener el link de pago de Mercado Pago", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PostMapping
    public ResponseEntity<CheckoutResponseDTO> procesar(@RequestBody CheckoutRequestDTO datos) {
        // Solo se puede comprar a nombre propio
        if (!seguridad.puedeAccederCliente(datos.getIdCliente())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        CheckoutResponseDTO respuesta = checkoutService.procesar(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // Errores de negocio -> 400 con un mensaje legible para mostrar en el frontend
    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> manejarError(CheckoutException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", e.getMessage()));
    }
}