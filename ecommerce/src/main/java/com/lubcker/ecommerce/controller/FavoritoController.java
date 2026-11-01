package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.model.Cliente;
import com.lubcker.ecommerce.model.Favorito;
import com.lubcker.ecommerce.model.Producto;
import com.lubcker.ecommerce.repository.ClienteRepository;
import com.lubcker.ecommerce.repository.FavoritoRepository;
import com.lubcker.ecommerce.repository.ProductoRepository;
import com.lubcker.ecommerce.security.SeguridadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Favoritos (❤️) del usuario con sesion. Siempre trabaja sobre el cliente del token,
// asi nadie puede ver ni tocar los favoritos de otro.
@Tag(name = "Favoritos", description = "Productos favoritos de cada cliente.")
@RestController
@RequestMapping("/api/favoritos")
public class FavoritoController {

    @Autowired
    private FavoritoRepository favoritoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private SeguridadService seguridad;

    // GET /api/favoritos -> ids de los productos favoritos
    @Operation(summary = "Listar los favoritos del usuario con sesión", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @GetMapping
    public List<Integer> listar() {
        return idsFavoritos(idCliente());
    }

    // PUT /api/favoritos/{idProducto} -> agrega (si ya estaba, no hace nada)
    @Operation(summary = "Agregar un producto a favoritos", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PutMapping("/{idProducto}")
    public ResponseEntity<Void> agregar(@PathVariable int idProducto) {
        return agregarSiFalta(idCliente(), idProducto)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // DELETE /api/favoritos/{idProducto}
    @Operation(summary = "Quitar un producto de favoritos", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @DeleteMapping("/{idProducto}")
    public ResponseEntity<Void> quitar(@PathVariable int idProducto) {
        favoritoRepository.findByClienteIdClienteAndProductoIdProducto(idCliente(), idProducto)
                .ifPresent(favoritoRepository::delete);
        return ResponseEntity.noContent().build();
    }

    // POST /api/favoritos/sincronizar  body: [1, 5, 7]
    // Suma a la cuenta los favoritos marcados sin sesion y devuelve la lista completa.
    @Operation(summary = "Sincronizar los favoritos guardados en el navegador con los del servidor", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PostMapping("/sincronizar")
    @Transactional
    public List<Integer> sincronizar(@RequestBody List<Integer> idsProductos) {
        int idCliente = idCliente();
        if (idsProductos != null) {
            idsProductos.stream().distinct().limit(200).forEach(id -> agregarSiFalta(idCliente, id));
        }
        return idsFavoritos(idCliente);
    }

    private boolean agregarSiFalta(int idCliente, int idProducto) {
        Producto producto = productoRepository.findById(idProducto).orElse(null);
        if (producto == null) return false;

        if (favoritoRepository.findByClienteIdClienteAndProductoIdProducto(idCliente, idProducto).isEmpty()) {
            Cliente cliente = clienteRepository.findById(idCliente).orElseThrow();
            Favorito favorito = new Favorito();
            favorito.setCliente(cliente);
            favorito.setProducto(producto);
            favoritoRepository.save(favorito);
        }
        return true;
    }

    private List<Integer> idsFavoritos(int idCliente) {
        return favoritoRepository.findByClienteIdCliente(idCliente).stream()
                .map(f -> f.getProducto().getIdProducto())
                .toList();
    }

    private int idCliente() {
        return seguridad.actual().orElseThrow().idCliente();
    }
}
