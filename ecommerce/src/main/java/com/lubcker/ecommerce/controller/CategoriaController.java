package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.CategoriaDTO;
import com.lubcker.ecommerce.dto.CategoriaRequestDTO;
import com.lubcker.ecommerce.model.Categoria;
import com.lubcker.ecommerce.repository.CategoriaRepository;
import com.lubcker.ecommerce.repository.ProductoRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Categorías", description = "Categorías de productos.")
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Operation(summary = "Listar categorías", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<CategoriaDTO> listar() {
        return categoriaRepository.findAll().stream()
                .sorted(Comparator.comparing(Categoria::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Obtener una categoría", description = "Acceso: **Público** (no requiere sesión).")
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDTO> obtener(@PathVariable int id) {
        return categoriaRepository.findById(id)
                .map(c -> ResponseEntity.ok(toDTO(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear una categoría", description = "Acceso: **Solo Admin**.")
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CategoriaRequestDTO datos) {
        String nombre = datos.getNombre().trim();
        if (existeNombre(nombre, -1)) return nombreRepetido();

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(categoriaRepository.save(categoria)));
    }

    @Operation(summary = "Editar una categoría", description = "Acceso: **Solo Admin**.")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @Valid @RequestBody CategoriaRequestDTO datos) {
        Categoria categoria = categoriaRepository.findById(id).orElse(null);
        if (categoria == null) return ResponseEntity.notFound().build();

        String nombre = datos.getNombre().trim();
        if (existeNombre(nombre, id)) return nombreRepetido();

        categoria.setNombre(nombre);
        return ResponseEntity.ok(toDTO(categoriaRepository.save(categoria)));
    }

    @Operation(summary = "Eliminar una categoría (no si tiene productos)", description = "Acceso: **Solo Admin**.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable int id) {
        if (!categoriaRepository.existsById(id)) return ResponseEntity.notFound().build();
        if (productoRepository.existsByCategoriaIdCategoria(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",
                    "La categoría tiene productos. Pasalos a otra categoría antes de eliminarla."));
        }
        categoriaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // El filtro del catalogo compara por nombre, asi que no puede haber dos iguales
    private boolean existeNombre(String nombre, int idExcluido) {
        return categoriaRepository.findAll().stream()
                .anyMatch(c -> c.getIdCategoria() != idExcluido && nombre.equalsIgnoreCase(c.getNombre()));
    }

    private ResponseEntity<?> nombreRepetido() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("mensaje", "Ya existe una categoría con ese nombre."));
    }

    private CategoriaDTO toDTO(Categoria c) {
        return new CategoriaDTO(c.getIdCategoria(), c.getNombre());
    }
}
