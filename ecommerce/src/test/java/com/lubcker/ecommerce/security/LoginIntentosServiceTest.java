package com.lubcker.ecommerce.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginIntentosServiceTest {

    // Reloj que se puede adelantar a mano para simular el paso del tiempo
    static class RelojManual extends Clock {
        Instant ahora = Instant.parse("2026-01-01T10:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }

        void avanzar(Duration d) { ahora = ahora.plus(d); }
    }

    private RelojManual reloj;
    private LoginIntentosService servicio;

    @BeforeEach
    void preparar() {
        reloj = new RelojManual();
        servicio = new LoginIntentosService(reloj);
    }

    private void fallar(String email, int veces) {
        for (int i = 0; i < veces; i++) servicio.registrarFallo(email);
    }

    @Test
    void menosDeCincoFallosNoBloquea() {
        fallar("a@b.com", LoginIntentosService.MAX_FALLOS - 1);
        assertEquals(0, servicio.minutosBloqueado("a@b.com"));
    }

    @Test
    void cincoFallosBloqueanQuinceMinutos() {
        fallar("a@b.com", LoginIntentosService.MAX_FALLOS);
        assertEquals(15, servicio.minutosBloqueado("a@b.com"));

        reloj.avanzar(Duration.ofMinutes(10));
        assertEquals(5, servicio.minutosBloqueado("a@b.com"));
    }

    @Test
    void elBloqueoVenceSolo() {
        fallar("a@b.com", LoginIntentosService.MAX_FALLOS);
        reloj.avanzar(LoginIntentosService.BLOQUEO.plusSeconds(1));

        assertEquals(0, servicio.minutosBloqueado("a@b.com"));
    }

    @Test
    void unLoginCorrectoReiniciaElContador() {
        fallar("a@b.com", LoginIntentosService.MAX_FALLOS - 1);
        servicio.registrarExito("a@b.com");
        fallar("a@b.com", LoginIntentosService.MAX_FALLOS - 1);

        assertEquals(0, servicio.minutosBloqueado("a@b.com"));
    }

    @Test
    void elCorreoNoDistingueMayusculasNiEspacios() {
        fallar(" A@B.com ", LoginIntentosService.MAX_FALLOS);
        assertTrue(servicio.minutosBloqueado("a@b.com") > 0);
    }

    @Test
    void elBloqueoDeUnCorreoNoAfectaAOtro() {
        fallar("a@b.com", LoginIntentosService.MAX_FALLOS);
        assertEquals(0, servicio.minutosBloqueado("otro@b.com"));
    }
}
