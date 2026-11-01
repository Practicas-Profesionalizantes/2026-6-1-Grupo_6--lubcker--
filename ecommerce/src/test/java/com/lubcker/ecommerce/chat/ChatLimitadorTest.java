package com.lubcker.ecommerce.chat;

import com.lubcker.ecommerce.chat.ChatLimitador.Resultado;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatLimitadorTest {

    static class RelojManual extends Clock {
        Instant ahora = Instant.parse("2026-01-01T10:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }

        void avanzar(Duration d) { ahora = ahora.plus(d); }
    }

    @Test
    void permiteHastaElLimitePorHoraYBloqueaElSiguiente() {
        ChatLimitador l = new ChatLimitador(3, 100, new RelojManual());

        for (int i = 0; i < 3; i++) assertEquals(Resultado.OK, l.intentar("ip1"));
        assertEquals(Resultado.LIMITE_PERSONAL, l.intentar("ip1"));
    }

    @Test
    void elLimiteEsPorPersona() {
        ChatLimitador l = new ChatLimitador(1, 100, new RelojManual());

        assertEquals(Resultado.OK, l.intentar("ip1"));
        assertEquals(Resultado.LIMITE_PERSONAL, l.intentar("ip1"));
        assertEquals(Resultado.OK, l.intentar("ip2"));
    }

    @Test
    void despuesDeUnaHoraSeLiberaElCupo() {
        RelojManual reloj = new RelojManual();
        ChatLimitador l = new ChatLimitador(1, 100, reloj);

        assertEquals(Resultado.OK, l.intentar("ip1"));
        assertEquals(Resultado.LIMITE_PERSONAL, l.intentar("ip1"));
        reloj.avanzar(Duration.ofMinutes(61));
        assertEquals(Resultado.OK, l.intentar("ip1"));
    }

    @Test
    void elLimiteDiarioGlobalFrenaATodos() {
        ChatLimitador l = new ChatLimitador(100, 2, new RelojManual());

        assertEquals(Resultado.OK, l.intentar("a"));
        assertEquals(Resultado.OK, l.intentar("b"));
        assertEquals(Resultado.LIMITE_GLOBAL, l.intentar("c"));
    }

    @Test
    void elLimiteGlobalSeLiberaAlDiaSiguiente() {
        RelojManual reloj = new RelojManual();
        ChatLimitador l = new ChatLimitador(100, 1, reloj);

        assertEquals(Resultado.OK, l.intentar("a"));
        assertEquals(Resultado.LIMITE_GLOBAL, l.intentar("b"));
        reloj.avanzar(Duration.ofHours(25));
        assertEquals(Resultado.OK, l.intentar("b"));
    }

    @Test
    void unMensajeRechazadoNoConsumeCupo() {
        RelojManual reloj = new RelojManual();
        ChatLimitador l = new ChatLimitador(1, 100, reloj);

        l.intentar("ip1");
        for (int i = 0; i < 5; i++) l.intentar("ip1"); // rechazados
        reloj.avanzar(Duration.ofMinutes(61));
        // si los rechazados hubieran contado, seguiria bloqueado
        assertEquals(Resultado.OK, l.intentar("ip1"));
    }
}
