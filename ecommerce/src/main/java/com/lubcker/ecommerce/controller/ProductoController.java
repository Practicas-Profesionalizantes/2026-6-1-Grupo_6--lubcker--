package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.ProductoDTO;
import com.lubcker.ecommerce.dto.ProductoRequestDTO;
import com.lubcker.ecommerce.model.Categoria;
import com.lubcker.ecommerce.model.Producto;
import com.lubcker.ecommerce.repository.CategoriaRepository;
import com.lubcker.ecommerce.repository.Detalle_OrdenRepository;
import com.lubcker.ecommerce.repository.FavoritoRepository;
import com.lubcker.ecommerce.repository.Ficha_TecnicaRepository;
import com.lubcker.ecommerce.repository.ProductoInsumoRepository;
import com.lubcker.ecommerce.repository.ProductoRepository;
import com.lubcker.ecommerce.repository.ResenasRepository;
import com.lubcker.ecommerce.security.SeguridadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Productos", description = "Catálogo de productos con unidad de medida, presentaciones y stock.")
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private Detalle_OrdenRepository detalleOrdenRepository;

    @Autowired
    private Ficha_TecnicaRepository fichaTecnicaRepository;

    @Autowired
    private ResenasRepository resenasRepository;

    @Autowired
    private FavoritoRepository favoritoRepository;

    @Autowired
    private ProductoInsumoRepository productoInsumoRepository;

    @Autowired
    private SeguridadService seguridad;

    // El catalogo solo muestra productos activos.
    // El panel de admin pide ?incluirInactivos=true para ver tambien los descontinuados.
    @Operation(summary = "Listar productos (los activos; con incluirInactivos=true también los descontinuados)", description = "Acceso: **Solo Admin**.")
    @GetMapping
    public List<ProductoDTO> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
        List<Producto> productos = incluirInactivos && seguridad.esAdmin()
                ? productoRepository.findAll()
                : productoRepository.findByActivoTrue();
        return productos.stream().map(this::toDTO).collect(Collectors.toList());
    }

    // Devuelve el producto aunque este inactivo: el detalle muestra "descontinuado"
    // (por ejemplo, si se llega desde un pedido viejo del historial).
    @Operation(summary = "Obtener un producto", description = "Acceso: **Público** (no requiere sesión).")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> obtener(@PathVariable int id) {
        return productoRepository.findById(id)
                .map(p -> ResponseEntity.ok(toDTO(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Listar los productos de una categoría", description = "Acceso: **Público** (no requiere sesión).")
    @GetMapping("/categoria/{idCategoria}")
    public List<ProductoDTO> listarPorCategoria(@PathVariable int idCategoria) {
        return productoRepository.findByCategoriaIdCategoriaAndActivoTrue(idCategoria)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Crear un producto", description = "Acceso: **Solo Admin**.")
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody ProductoRequestDTO datos) {
        Categoria categoria = categoriaRepository.findById(datos.getIdCategoria()).orElse(null);
        if (categoria == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La categoría elegida no existe."));
        }

        Producto producto = new Producto();
        String error = aplicarDatos(producto, datos, categoria);
        if (error != null) return ResponseEntity.badRequest().body(Map.of("mensaje", error));
        producto.setActivo(datos.getActivo() == null || datos.getActivo());

        Producto guardado = productoRepository.save(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardado));
    }

    @Operation(summary = "Editar un producto", description = "Acceso: **Solo Admin**.")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @Valid @RequestBody ProductoRequestDTO datos) {
        Producto producto = productoRepository.findById(id).orElse(null);
        if (producto == null) return ResponseEntity.notFound().build();

        Categoria categoria = categoriaRepository.findById(datos.getIdCategoria()).orElse(null);
        if (categoria == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La categoría elegida no existe."));
        }

        String error = aplicarDatos(producto, datos, categoria);
        if (error != null) return ResponseEntity.badRequest().body(Map.of("mensaje", error));
        if (datos.getActivo() != null) producto.setActivo(datos.getActivo());

        return ResponseEntity.ok(toDTO(productoRepository.save(producto)));
    }

    // PUT /api/productos/{id}/activo  body: {"activo": false} -> descontinuar o reactivar
    @Operation(summary = "Activar o desactivar (descontinuar) un producto", description = "Acceso: **Solo Admin**.")
    @PutMapping("/{id}/activo")
    public ResponseEntity<?> cambiarActivo(@PathVariable int id, @RequestBody Map<String, Boolean> cuerpo) {
        Boolean activo = cuerpo.get("activo");
        if (activo == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Falta el campo \"activo\"."));
        }
        return productoRepository.findById(id)
                .<ResponseEntity<?>>map(p -> {
                    p.setActivo(activo);
                    return ResponseEntity.ok(toDTO(productoRepository.save(p)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Solo se puede borrar un producto que nunca se vendio. Si tiene ventas, se desactiva.
    @Operation(summary = "Eliminar un producto (no si tiene ventas)", description = "Acceso: **Solo Admin**.")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable int id) {
        if (!productoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (detalleOrdenRepository.existsByProductoIdProducto(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",
                    "Este producto tiene ventas registradas y no se puede eliminar. Desactivalo para que deje de mostrarse."));
        }
        // Datos que dependen del producto
        fichaTecnicaRepository.borrarPorProducto(id);
        resenasRepository.borrarPorProducto(id);
        favoritoRepository.borrarPorProducto(id);
        productoInsumoRepository.borrarPorProducto(id); // su materia prima cargada

        productoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    static final List<String> UNIDADES = List.of("unidad", "kg", "litro", "metro");
    static final int MAX_MEDIDAS = 12;
    static final int MAX_MEDIDA = 100_000;
    static final long MAX_PRECIO_FINAL = 100_000_000L;

    // Copia los datos al producto. Devuelve un mensaje de error si algo no es valido (o null si esta todo bien).
    private String aplicarDatos(Producto producto, ProductoRequestDTO datos, Categoria categoria) {
        String unidad = producto.getUnidadMedida();
        if (datos.getUnidadMedida() != null) {
            unidad = datos.getUnidadMedida().trim().toLowerCase();
            if (unidad.isEmpty()) unidad = "unidad";
            if (!UNIDADES.contains(unidad)) {
                return "La unidad de medida no es válida. Opciones: " + String.join(", ", UNIDADES) + ".";
            }
        }

        String medidas = producto.getPresentaciones();
        if (datos.getPresentaciones() != null) {
            List<Integer> lista = new ArrayList<>(datos.getPresentaciones().stream()
                    .filter(java.util.Objects::nonNull).distinct().sorted().toList());
            if (lista.isEmpty()) lista.add(1);
            if (lista.size() > MAX_MEDIDAS) return "Podés cargar hasta " + MAX_MEDIDAS + " medidas de venta.";
            if (lista.get(0) < 1 || lista.get(lista.size() - 1) > MAX_MEDIDA) {
                return "Las medidas de venta tienen que ser números enteros entre 1 y " + MAX_MEDIDA + ".";
            }
            medidas = lista.stream().map(String::valueOf).collect(Collectors.joining(","));
        }

        // El precio de la medida mas grande tiene que entrar en un entero con margen
        int mayor = java.util.Arrays.stream(medidas.split(",")).mapToInt(Integer::parseInt).max().orElse(1);
        if ((long) datos.getPrecio() * mayor > MAX_PRECIO_FINAL) {
            return "El precio de la medida más grande es demasiado alto. Revisá el precio o las medidas.";
        }

        producto.setNombre(datos.getNombre().trim());
        producto.setPrecio(datos.getPrecio());
        producto.setCategoria(categoria);
        producto.setImagen(datos.getImagen());
        producto.setStock(datos.getStock());
        producto.setUnidadMedida(unidad);
        producto.setPresentaciones(medidas);
        return null;
    }

    private ProductoDTO toDTO(Producto p) {
        return new ProductoDTO(
                p.getIdProducto(),
                p.getNombre(),
                p.getPrecio(),
                p.getCategoria() != null ? p.getCategoria().getIdCategoria() : 0,
                p.getCategoria() != null ? p.getCategoria().getNombre() : null,
                p.getImagen(),
                p.getStock(),
                p.isActivo(),
                p.getUnidadMedida(),
                p.listaPresentaciones()
        );
    }
}
