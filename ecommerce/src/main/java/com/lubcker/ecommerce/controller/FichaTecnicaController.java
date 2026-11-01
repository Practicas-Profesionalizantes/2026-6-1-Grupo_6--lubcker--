package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.FichaTecnicaDTO;
import com.lubcker.ecommerce.dto.FichaTecnicaRequestDTO;
import com.lubcker.ecommerce.model.Ficha_Tecnica;
import com.lubcker.ecommerce.model.Producto;
import com.lubcker.ecommerce.repository.Ficha_TecnicaRepository;
import com.lubcker.ecommerce.repository.ProductoRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Fichas técnicas", description = "Descripción, modelo y presentación de cada producto.")
@RestController
@RequestMapping("/api/fichas-tecnicas")
public class FichaTecnicaController {

    @Autowired
    private Ficha_TecnicaRepository fichaTecnicaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Operation(summary = "Listar fichas técnicas", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<FichaTecnicaDTO> listar() {
        return fichaTecnicaRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Operation(summary = "Obtener una ficha técnica", description = "Acceso: **Público** (no requiere sesión).")
    @GetMapping("/{id}")
    public ResponseEntity<FichaTecnicaDTO> obtener(@PathVariable int id) {
        return fichaTecnicaRepository.findById(id)
                .map(f -> ResponseEntity.ok(toDTO(f)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/fichas-tecnicas/producto/{idProducto} -> ficha tecnica de un producto puntual
    @Operation(summary = "Obtener la ficha técnica de un producto", description = "Acceso: **Público** (no requiere sesión).")
    @GetMapping("/producto/{idProducto}")
    public ResponseEntity<FichaTecnicaDTO> obtenerPorProducto(@PathVariable int idProducto) {
        return fichaTecnicaRepository.findByProductoIdProducto(idProducto)
                .map(f -> ResponseEntity.ok(toDTO(f)))
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/fichas-tecnicas/producto/{idProducto} -> crea la ficha si no existe o la actualiza.
    // Es lo que usa el panel de admin al guardar un producto.
    @Operation(summary = "Crear o reemplazar la ficha técnica de un producto", description = "Acceso: **Solo Admin**.")
    @PutMapping("/producto/{idProducto}")
    public ResponseEntity<FichaTecnicaDTO> guardarPorProducto(@PathVariable int idProducto,
                                                              @Valid @RequestBody FichaTecnicaRequestDTO datos) {
        Producto producto = productoRepository.findById(idProducto).orElse(null);
        if (producto == null) return ResponseEntity.notFound().build();

        Ficha_Tecnica ficha = fichaTecnicaRepository.findByProductoIdProducto(idProducto).orElseGet(() -> {
            Ficha_Tecnica nueva = new Ficha_Tecnica();
            nueva.setProducto(producto);
            return nueva;
        });
        ficha.setModelo(vacioANull(datos.getModelo()));
        ficha.setPresentacion(vacioANull(datos.getPresentacion()));
        ficha.setDescripcion(vacioANull(datos.getDescripcion()));
        return ResponseEntity.ok(toDTO(fichaTecnicaRepository.save(ficha)));
    }

    @Operation(summary = "Crear una ficha técnica", description = "Acceso: **Solo Admin**.")
    @PostMapping
    public ResponseEntity<FichaTecnicaDTO> crear(@Valid @RequestBody FichaTecnicaRequestDTO datos) {
        Producto producto = productoRepository.findById(datos.getIdProducto())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + datos.getIdProducto()));

        Ficha_Tecnica ficha = new Ficha_Tecnica();
        ficha.setProducto(producto);
        ficha.setModelo(datos.getModelo());
        ficha.setPresentacion(datos.getPresentacion());
        ficha.setDescripcion(datos.getDescripcion());

        Ficha_Tecnica guardada = fichaTecnicaRepository.save(ficha);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardada));
    }

    @Operation(summary = "Editar una ficha técnica", description = "Acceso: **Solo Admin**.")
    @PutMapping("/{id}")
    public ResponseEntity<FichaTecnicaDTO> actualizar(@PathVariable int id, @Valid @RequestBody FichaTecnicaRequestDTO datos) {
        return fichaTecnicaRepository.findById(id)
                .map(ficha -> {
                    ficha.setModelo(datos.getModelo());
                    ficha.setPresentacion(datos.getPresentacion());
                    ficha.setDescripcion(datos.getDescripcion());
                    return ResponseEntity.ok(toDTO(fichaTecnicaRepository.save(ficha)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar una ficha técnica", description = "Acceso: **Solo Admin**.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        return fichaTecnicaRepository.findById(id)
                .map(ficha -> {
                    Producto producto = ficha.getProducto();
                    if (producto != null) {
                        producto.setFichaTecnica(null);
                        productoRepository.save(producto);
                    }
                    fichaTecnicaRepository.delete(ficha);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private FichaTecnicaDTO toDTO(Ficha_Tecnica f) {
        return new FichaTecnicaDTO(
                f.getIdFicha(),
                f.getProducto() != null ? f.getProducto().getIdProducto() : 0,
                f.getModelo(),
                f.getPresentacion(),
                f.getDescripcion()
        );
    }
}