package com.lubcker.ecommerce.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Frena a quien prueba contraseñas: tras MAX_FALLOS intentos fallidos seguidos sobre un
// mismo correo, ese correo queda bloqueado durante BLOQUEO.
// Se guarda en memoria: si se reinicia el backend, los contadores vuelven a cero.
@Service
public class LoginIntentosService {

    static final int MAX_FALLOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Registro(int fallos, Instant bloqueadoHasta) {}

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LoginIntentosService() {
        this(Clock.systemUTC());
    }

    // Para los tests: permite simular el paso del tiempo
    LoginIntentosService(Clock reloj) {
        this.reloj = reloj;
    }

    // Minutos que faltan para desbloquear (0 si no esta bloqueado)
    public long minutosBloqueado(String email) {
        Registro r = registros.get(clave(email));
        if (r == null || r.bloqueadoHasta() == null) return 0;

        Instant ahora = reloj.instant();
        if (!ahora.isBefore(r.bloqueadoHasta())) {
            registros.remove(clave(email)); // el bloqueo ya vencio
            return 0;
        }
        long segundos = Duration.between(ahora, r.bloqueadoHasta()).getSeconds();
        return Math.max(1, (segundos + 59) / 60);
    }

    public void registrarFallo(String email) {
        registros.compute(clave(email), (k, r) -> {
            int fallos = (r == null ? 0 : r.fallos()) + 1;
            return fallos >= MAX_FALLOS
                    ? new Registro(0, reloj.instant().plus(BLOQUEO))
                    : new Registro(fallos, null);
        });
    }

    public void registrarExito(String email) {
        registros.remove(clave(email));
    }

    private String clave(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
