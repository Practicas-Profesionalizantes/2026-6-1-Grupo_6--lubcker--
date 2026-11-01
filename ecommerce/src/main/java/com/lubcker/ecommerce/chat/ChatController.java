package com.lubcker.ecommerce.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lubcker.ecommerce.security.SeguridadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// POST /api/chat  -> publico. Si el pedido trae un token valido, el asistente sabe quien es
// el usuario (lo necesita para el estado de pedidos); si no, responde como visitante.
@Tag(name = "Asistente Lubcki", description = "Chat con el asistente virtual (IA). Con sesión puede informar el estado de los pedidos propios.")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    static final int MAX_CARACTERES_USUARIO = 500;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatLimitador limitador;

    @Autowired
    private SeguridadService seguridad;

    @Operation(summary = "Enviar una pregunta al asistente Lubcki (máx. 500 caracteres por mensaje; 30 mensajes por hora por persona)", description = "Acceso: **Público** (no requiere sesión).")
    @PostMapping
    public ResponseEntity<Map<String, String>> preguntar(@Valid @RequestBody ChatRequestDTO datos,
                                                         HttpServletRequest request) {
        List<MensajeChat> mensajes = datos.getMensajes();

        // Tiene que terminar con una pregunta del usuario
        if (!"user".equals(mensajes.get(mensajes.size() - 1).getRol())) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El último mensaje tiene que ser del usuario."));
        }
        for (MensajeChat m : mensajes) {
            if ("user".equals(m.getRol()) && m.getTexto().length() > MAX_CARACTERES_USUARIO) {
                return ResponseEntity.badRequest().body(Map.of("mensaje",
                        "Los mensajes pueden tener hasta " + MAX_CARACTERES_USUARIO + " caracteres."));
            }
        }
        if (!chatService.disponible()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("mensaje", "El asistente todavía no está disponible. Probá más tarde."));
        }

        // Con sesion se limita por usuario; sin sesion, por direccion IP
        String clave = seguridad.actual().map(u -> "u" + u.idUsuario()).orElse("ip" + request.getRemoteAddr());
        switch (limitador.intentar(clave)) {
            case LIMITE_PERSONAL -> {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("mensaje", "Hiciste muchas consultas seguidas. Probá de nuevo en un rato."));
            }
            case LIMITE_GLOBAL -> {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("mensaje", "El asistente alcanzó su límite diario. Probá de nuevo mañana."));
            }
            default -> { }
        }

        try {
            return ResponseEntity.ok(Map.of("respuesta", chatService.responder(mensajes)));
        } catch (ChatNoDisponibleException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("mensaje", e.getMessage()));
        }
    }
}
