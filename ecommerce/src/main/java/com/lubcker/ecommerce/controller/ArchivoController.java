package com.lubcker.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Subida de imagenes (fotos de productos y de perfil). Se guardan en la carpeta
// "uploads" y se sirven en /uploads/** (ver WebConfig).
@Tag(name = "Archivos", description = "Subida de imágenes (productos, perfil y banners).")
@RestController
@RequestMapping("/api/archivos")
public class ArchivoController {

    public static final Path CARPETA = Paths.get("uploads").toAbsolutePath().normalize();
    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp", "gif");

    @Operation(summary = "Subir una imagen (jpg, png, webp o gif, hasta 5 MB)", description = "Acceso: **Sesión** (cliente o admin). Un cliente solo puede ver y modificar sus propios datos; el admin accede a todo.")
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subir(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo está vacío"));
        }

        String original = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
        int punto = original.lastIndexOf('.');
        String extension = punto >= 0 ? original.substring(punto + 1).toLowerCase(Locale.ROOT) : "";
        String tipo = archivo.getContentType() == null ? "" : archivo.getContentType();

        if (!EXTENSIONES.contains(extension) || !tipo.startsWith("image/")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(Map.of("error", "Solo se permiten imágenes (jpg, png, webp o gif)"));
        }

        // El nombre lo genera el servidor: nunca se usa el nombre que manda el cliente.
        String nombre = UUID.randomUUID() + "." + extension;
        Files.createDirectories(CARPETA);
        Files.copy(archivo.getInputStream(), CARPETA.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("ruta", "uploads/" + nombre));
    }
}
