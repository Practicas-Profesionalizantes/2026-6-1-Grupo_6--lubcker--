package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.InsumoDTO;
import com.lubcker.ecommerce.dto.InsumoRequestDTO;
import com.lubcker.ecommerce.model.Insumo;
import com.lubcker.ecommerce.repository.InsumoRepository;
import com.lubcker.ecommerce.repository.ProductoInsumoRepository;
import com.lubcker.ecommerce.service.RecetaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

// Gestor de insumos (materia prima). Todo /api/admin/** es solo ADMIN (regla general de SecurityConfig).
@Tag(name = "Insumos (admin)", description = "Materia prima usada para fabricar los productos, con su costo por unidad.")
@RestController
@RequestMapping("/api/admin/insumos")
public class InsumoController {

    @Autowired
    private InsumoRepository insumoRepository;

    @Autowired
    private ProductoInsumoRepository recetaRepository;

    @Operation(summary = "Listar insumos", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<InsumoDTO> listar() {
        return insumoRepository.findAllByOrderByNombreAsc().stream().map(this::aDTO).toList();
    }

    @Operation(summary = "Crear un insumo (el costo por unidad es obligatorio)", description = "Acceso: **Solo Admin**.")
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody InsumoRequestDTO datos) {
        String nombre = datos.getNombre().trim();
        String error = validar(datos);
        if (error == null && insumoRepository.existsByNombreIgnoreCase(nombre)) {
            error = "Ya cargaste un insumo con ese nombre.";
        }
        if (error != null) return ResponseEntity.badRequest().body(Map.of("mensaje", error));

        Insumo insumo = new Insumo();
        aplicar(insumo, datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(aDTO(insumoRepository.save(insumo)));
    }

    @Operation(summary = "Editar un insumo (si está en uso no se puede cambiar su unidad)", description = "Acceso: **Solo Admin**.")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @Valid @RequestBody InsumoRequestDTO datos) {
        Insumo insumo = insumoRepository.findById(id).orElse(null);
        if (insumo == null) return ResponseEntity.notFound().build();

        String error = validar(datos);
        if (error == null && insumoRepository.existsByNombreIgnoreCaseAndIdInsumoNot(datos.getNombre().trim(), id)) {
            error = "Ya cargaste un insumo con ese nombre.";
        }
        if (error != null) return ResponseEntity.badRequest().body(Map.of("mensaje", error));

        // Si ya esta en recetas, cambiar la unidad (de kg a g, por ejemplo) falsearia todas las cantidades
        long enUso = recetaRepository.countByInsumoIdInsumo(id);
        if (enUso > 0 && !insumo.getUnidadMedida().equals(datos.getUnidadMedida().trim().toLowerCase())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",
                    "Este insumo está en la materia prima de " + enUso + (enUso == 1 ? " producto" : " productos")
                            + ": no se puede cambiar su unidad de medida."));
        }

        aplicar(insumo, datos);
        return ResponseEntity.ok(aDTO(insumoRepository.save(insumo)));
    }

    @Operation(summary = "Eliminar un insumo (no si está en uso)", description = "Acceso: **Solo Admin**.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable int id) {
        if (!insumoRepository.existsById(id)) return ResponseEntity.notFound().build();
        long enUso = recetaRepository.countByInsumoIdInsumo(id);
        if (enUso > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",
                    "Este insumo está en la materia prima de " + enUso + (enUso == 1 ? " producto" : " productos")
                            + ". Quitalo de esas recetas antes de eliminarlo."));
        }
        insumoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String validar(InsumoRequestDTO datos) {
        String unidad = datos.getUnidadMedida().trim().toLowerCase();
        if (!RecetaService.UNIDADES_INSUMO.contains(unidad)) {
            return "La unidad de medida no es válida. Opciones: " + String.join(", ", RecetaService.UNIDADES_INSUMO) + ".";
        }
        return null;
    }

    private void aplicar(Insumo insumo, InsumoRequestDTO datos) {
        insumo.setNombre(datos.getNombre().trim());
        insumo.setUnidadMedida(datos.getUnidadMedida().trim().toLowerCase());
        insumo.setCostoUnitario(datos.getCostoUnitario().setScale(2, RoundingMode.HALF_UP));
        String proveedor = datos.getProveedor() == null ? "" : datos.getProveedor().trim();
        insumo.setProveedor(proveedor.isEmpty() ? null : proveedor);
    }

    private InsumoDTO aDTO(Insumo i) {
        return new InsumoDTO(i.getIdInsumo(), i.getNombre(), i.getUnidadMedida(), i.getCostoUnitario(),
                i.getProveedor(), recetaRepository.countByInsumoIdInsumo(i.getIdInsumo()));
    }
}
