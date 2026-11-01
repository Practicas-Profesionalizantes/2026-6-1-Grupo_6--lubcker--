package com.lubcker.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

// Todos los errores salen como JSON {"mensaje": "..."} y nunca muestran detalles internos
// (nombres de clases, SQL, etc.). El detalle real queda solo en el log del servidor.
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private ResponseEntity<Map<String, String>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getDefaultMessage())
                .orElse("Los datos enviados no son válidos.");
        return error(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> cuerpoIlegible(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> archivoGrande(MaxUploadSizeExceededException e) {
        return error(HttpStatus.CONTENT_TOO_LARGE, "El archivo supera el tamaño máximo (5 MB).");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflictoDatos(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, "No se pudo completar la operación: hay datos relacionados o repetidos.");
    }

    // Los errores de seguridad los resuelve Spring Security: se relanzan como 401/403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> sinPermiso(AccessDeniedException e) {
        return error(HttpStatus.FORBIDDEN, "No tenés permiso para hacer esto.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> sinSesion(AuthenticationException e) {
        return error(HttpStatus.UNAUTHORIZED, "Tenés que iniciar sesión.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> inesperado(Exception e) {
        // Errores "normales" de Spring (ruta inexistente, metodo no permitido, etc.): se respeta su codigo
        if (e instanceof ErrorResponse er) {
            HttpStatus estado = HttpStatus.resolve(er.getStatusCode().value());
            return error(estado != null ? estado : HttpStatus.BAD_REQUEST, "No se pudo procesar la petición.");
        }
        log.error("Error inesperado", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intentá de nuevo.");
    }
}
