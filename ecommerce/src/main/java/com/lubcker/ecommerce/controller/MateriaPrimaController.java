package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.RecetaDTO;
import com.lubcker.ecommerce.dto.RecetaRequestDTO;
import com.lubcker.ecommerce.dto.ResumenCostoDTO;
import com.lubcker.ecommerce.service.RecetaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// "Materia prima de productos": que insumos (y cuanto de cada uno) lleva cada producto publicado,
// con su costo de fabricacion y ganancia. Solo ADMIN (todo /api/admin/** lo exige SecurityConfig).
// Estos datos NUNCA van en /api/productos, que es publico.
@Tag(name = "Materia prima (admin)", description = "Receta de cada producto y cálculo de costo de fabricación, ganancia y margen.")
@RestController
@RequestMapping("/api/admin/materia-prima")
public class MateriaPrimaController {

    @Autowired
    private RecetaService recetaService;

    // Costo y ganancia por unidad de todos los productos con materia prima cargada
    @Operation(summary = "Costo, ganancia y margen por unidad de todos los productos con receta", description = "Acceso: **Solo Admin**.")
    @GetMapping("/resumen")
    public List<ResumenCostoDTO> resumen() {
        return recetaService.resumen();
    }

    @Operation(summary = "Obtener la receta de un producto con su costo de fabricación", description = "Acceso: **Solo Admin**.")
    @GetMapping("/producto/{idProducto}")
    public ResponseEntity<RecetaDTO> obtener(@PathVariable int idProducto) {
        return recetaService.obtener(idProducto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Reemplaza la materia prima del producto. Lista vacia = dejarlo sin materia prima.
    @Operation(summary = "Reemplazar la receta completa de un producto", description = "Acceso: **Solo Admin**.")
    @PutMapping("/producto/{idProducto}")
    public ResponseEntity<?> guardar(@PathVariable int idProducto, @Valid @RequestBody RecetaRequestDTO datos) {
        String error = recetaService.guardar(idProducto, datos.getItems());
        if (error != null) return ResponseEntity.badRequest().body(Map.of("mensaje", error));
        return ResponseEntity.ok(recetaService.obtener(idProducto).orElseThrow());
    }
}
