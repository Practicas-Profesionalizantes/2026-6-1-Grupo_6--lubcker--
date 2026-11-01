package com.lubcker.ecommerce.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String CLAVE = "clave-de-prueba-con-mas-de-32-caracteres-123";

    @Test
    void unTokenGeneradoSeValidaYDevuelveElUsuario() {
        JwtService jwt = new JwtService(CLAVE, 8);
        String token = jwt.generar(42);

        assertEquals(42, jwt.validar(token).orElseThrow());
    }

    @Test
    void unTokenModificadoNoEsValido() {
        JwtService jwt = new JwtService(CLAVE, 8);
        String token = jwt.generar(42);
        // Se cambia un caracter de la firma
        char ultimo = token.charAt(token.length() - 1);
        String alterado = token.substring(0, token.length() - 1) + (ultimo == 'A' ? 'B' : 'A');

        assertTrue(jwt.validar(alterado).isEmpty());
    }

    @Test
    void unTokenFirmadoConOtraClaveNoEsValido() {
        String token = new JwtService("otra-clave-distinta-tambien-de-32-caracteres!", 8).generar(42);

        assertTrue(new JwtService(CLAVE, 8).validar(token).isEmpty());
    }

    @Test
    void unTokenVencidoNoEsValido() {
        JwtService vencido = new JwtService(CLAVE, -1); // vence una hora antes de emitirse
        String token = vencido.generar(42);

        assertTrue(vencido.validar(token).isEmpty());
    }

    @Test
    void textoQueNoEsTokenNoEsValido() {
        assertTrue(new JwtService(CLAVE, 8).validar("esto-no-es-un-token").isEmpty());
    }

    @Test
    void unaClaveCortaSeRechazaAlArrancar() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corta", 8));
    }

    @Test
    void sinClaveConfiguradaFuncionaConUnaTemporal() {
        JwtService jwt = new JwtService("", 8);
        assertEquals(7, jwt.validar(jwt.generar(7)).orElseThrow());
    }
}
