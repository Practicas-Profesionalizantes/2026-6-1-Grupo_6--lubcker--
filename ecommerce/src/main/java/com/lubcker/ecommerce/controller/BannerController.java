package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.dto.BannerDTO;
import com.lubcker.ecommerce.dto.BannerRequestDTO;
import com.lubcker.ecommerce.model.Banner;
import com.lubcker.ecommerce.repository.BannerRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Carrusel de la pagina principal.
//  - GET /api/banners            -> publico: solo los activos, en orden
//  - /api/admin/banners/**       -> solo ADMIN (regla general de SecurityConfig)
@Tag(name = "Carrusel", description = "Banners de la página de inicio. La lectura es pública; la gestión es del admin.")
@RestController
public class BannerController {

    @Autowired
    private BannerRepository bannerRepository;

    @Operation(summary = "Listar los banners activos del carrusel, en orden", description = "Acceso: **Público** (no requiere sesión).")
    @GetMapping("/api/banners")
    public List<BannerDTO> publicos() {
        return bannerRepository.findByActivoTrueOrderByOrdenAscIdBannerAsc().stream().map(this::aDTO).toList();
    }

    @Operation(summary = "Listar todos los banners (activos e inactivos)", description = "Acceso: **Solo Admin**.")
    @GetMapping("/api/admin/banners")
    public List<BannerDTO> todos() {
        return bannerRepository.findAllByOrderByOrdenAscIdBannerAsc().stream().map(this::aDTO).toList();
    }

    @Operation(summary = "Crear un banner", description = "Acceso: **Solo Admin**.")
    @PostMapping("/api/admin/banners")
    public ResponseEntity<?> crear(@Valid @RequestBody BannerRequestDTO datos) {
        String error = validarEnlace(datos.getEnlace());
        if (error != null) return ResponseEntity.badRequest().body(Map.of("mensaje", error));

        Banner banner = new Banner();
        aplicar(banner, datos);
        banner.setActivo(datos.getActivo() == null || datos.getActivo());
        // Va al final del carrusel
        banner.setOrden(bannerRepository.findAll().stream().mapToInt(Banner::getOrden).max().orElse(0) + 1);
        return ResponseEntity.status(HttpStatus.CREATED).body(aDTO(bannerRepository.save(banner)));
    }

    @Operation(summary = "Editar un banner", description = "Acceso: **Solo Admin**.")
    @PutMapping("/api/admin/banners/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @Valid @RequestBody BannerRequestDTO datos) {
        Banner banner = bannerRepository.findById(id).orElse(null);
        if (banner == null) return ResponseEntity.notFound().build();

        String error = validarEnlace(datos.getEnlace());
        if (error != null) return ResponseEntity.badRequest().body(Map.of("mensaje", error));

        aplicar(banner, datos);
        if (datos.getActivo() != null) banner.setActivo(datos.getActivo());
        return ResponseEntity.ok(aDTO(bannerRepository.save(banner)));
    }

    // PUT /api/admin/banners/orden  body: [3, 1, 2] -> ids en el orden deseado
    @Operation(summary = "Reordenar los banners (lista de ids en el orden deseado)", description = "Acceso: **Solo Admin**.")
    @PutMapping("/api/admin/banners/orden")
    @Transactional
    public ResponseEntity<?> reordenar(@RequestBody List<Integer> ids) {
        List<Banner> banners = bannerRepository.findAll();
        if (ids == null || ids.size() != banners.size()
                || !banners.stream().map(Banner::getIdBanner).allMatch(ids::contains)) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La lista de banners no coincide."));
        }
        for (Banner b : banners) b.setOrden(ids.indexOf(b.getIdBanner()) + 1);
        bannerRepository.saveAll(banners);
        return ResponseEntity.ok(todos());
    }

    // Se borra el banner; el archivo de imagen queda en uploads (puede estar usado en otro lado)
    @Operation(summary = "Eliminar un banner", description = "Acceso: **Solo Admin**.")
    @DeleteMapping("/api/admin/banners/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable int id) {
        if (!bannerRepository.existsById(id)) return ResponseEntity.notFound().build();
        bannerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void aplicar(Banner banner, BannerRequestDTO datos) {
        banner.setImagen(datos.getImagen().trim());
        banner.setTitulo(vacioANull(datos.getTitulo()));
        banner.setEnlace(vacioANull(datos.getEnlace()));
    }

    // Solo rutas internas (ej: /producto/5): evita que se cargue un enlace a otro sitio
    private String validarEnlace(String enlace) {
        String e = vacioANull(enlace);
        if (e != null && (!e.startsWith("/") || e.startsWith("//"))) {
            return "El enlace tiene que ser una ruta interna de la tienda (ej: /producto/5).";
        }
        return null;
    }

    private String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private BannerDTO aDTO(Banner b) {
        return new BannerDTO(b.getIdBanner(), b.getImagen(), b.getTitulo(), b.getEnlace(), b.getOrden(), b.isActivo());
    }
}
