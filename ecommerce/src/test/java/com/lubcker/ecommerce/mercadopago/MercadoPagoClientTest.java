package com.lubcker.ecommerce.mercadopago;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MercadoPagoClientTest {

    @Test
    void sinTokenNoEstaConfigurado() {
        assertFalse(new MercadoPagoClient("", "http://localhost:1").configurado());
        assertFalse(new MercadoPagoClient("   ", "http://localhost:1").configurado());
        assertTrue(new MercadoPagoClient("TEST-123", "http://localhost:1").configurado());
    }

    @Test
    void sinTokenLasLlamadasFallanSinSalirALaRed() {
        MercadoPagoClient c = new MercadoPagoClient("", "http://localhost:1");
        assertThrows(MercadoPagoException.class, () -> c.obtenerPago("123"));
        assertThrows(MercadoPagoException.class, () -> c.crearPreferencia(java.util.Map.of()));
    }

    @Test
    void unIdQueNoEsNumericoNuncaSeConsulta() {
        // Si se consultara, intentaria conectarse al puerto 1 y fallaria con MercadoPagoException
        MercadoPagoClient c = new MercadoPagoClient("TEST-123", "http://localhost:1");
        assertNull(c.obtenerPago("../v1/users/me"));
        assertNull(c.obtenerPago("12/../34"));
        assertNull(c.obtenerPago(""));
        assertNull(c.obtenerPago(null));
    }
}
