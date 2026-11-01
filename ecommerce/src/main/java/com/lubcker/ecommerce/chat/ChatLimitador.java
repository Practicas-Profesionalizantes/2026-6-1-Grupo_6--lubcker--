package com.lubcker.ecommerce.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Cada mensaje del chat cuesta plata (API de IA) y el chat es publico, asi que se limita:
//  - por persona (usuario con sesion, o direccion IP si no hay): X mensajes por hora
//  - global: Y mensajes por dia entre todos
// Los contadores viven en memoria: si se reinicia el backend, vuelven a cero.
@Service
public class ChatLimitador {

    public enum Resultado { OK, LIMITE_PERSONAL, LIMITE_GLOBAL }

    private static final Duration VENTANA = Duration.ofHours(1);
    private static final Duration DIA = Duration.ofHours(24);

    private final int maxPorHora;
    private final int maxPorDia;
    private final Clock reloj;

    private final Map<String, Deque<Instant>> porClave = new ConcurrentHashMap<>();
    private final Deque<Instant> global = new ArrayDeque<>();

    @org.springframework.beans.factory.annotation.Autowired
    public ChatLimitador(@Value("${app.chat.limite-por-hora:30}") int maxPorHora,
                         @Value("${app.chat.limite-por-dia:2000}") int maxPorDia) {
        this(maxPorHora, maxPorDia, Clock.systemUTC());
    }

    // Para los tests: permite simular el paso del tiempo
    ChatLimitador(int maxPorHora, int maxPorDia, Clock reloj) {
        this.maxPorHora = maxPorHora;
        this.maxPorDia = maxPorDia;
        this.reloj = reloj;
    }

    // Anota un mensaje y dice si se puede procesar
    public Resultado intentar(String clave) {
        Instant ahora = reloj.instant();

        synchronized (global) {
            purgar(global, ahora.minus(DIA));
            if (global.size() >= maxPorDia) return Resultado.LIMITE_GLOBAL;
        }

        Deque<Instant> cola = porClave.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            purgar(cola, ahora.minus(VENTANA));
            if (cola.size() >= maxPorHora) return Resultado.LIMITE_PERSONAL;
            cola.addLast(ahora);
        }
        synchronized (global) {
            global.addLast(ahora);
        }

        // Limpieza ocasional de claves sin actividad para que el mapa no crezca sin fin
        if (porClave.size() > 5000) {
            porClave.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    purgar(e.getValue(), ahora.minus(VENTANA));
                    return e.getValue().isEmpty();
                }
            });
        }
        return Resultado.OK;
    }

    private void purgar(Deque<Instant> cola, Instant desde) {
        while (!cola.isEmpty() && cola.peekFirst().isBefore(desde)) cola.pollFirst();
    }
}
