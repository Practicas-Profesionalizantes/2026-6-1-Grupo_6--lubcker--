package com.lubcker.ecommerce.mercadopago;

import com.lubcker.ecommerce.model.*;
import com.lubcker.ecommerce.repository.EnvioRepository;
import com.lubcker.ecommerce.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MercadoPagoServiceTest {

    private MercadoPagoClient cliente;
    private PagoRepository pagoRepository;
    private EnvioRepository envioRepository;
    private OrdenCancelacionService cancelacion;
    private MercadoPagoService servicio;

    private Pago pago;
    private Envio envio;

    @BeforeEach
    void preparar() {
        cliente = mock(MercadoPagoClient.class);
        pagoRepository = mock(PagoRepository.class);
        envioRepository = mock(EnvioRepository.class);
        cancelacion = mock(OrdenCancelacionService.class);
        servicio = new MercadoPagoService(cliente, pagoRepository, envioRepository, cancelacion,
                "http://localhost:3000/", "", 24);

        Cliente c = new Cliente();
        c.setIdCliente(7);
        Orden orden = new Orden();
        orden.setIdOrden(42);
        orden.setCliente(c);
        pago = new Pago();
        pago.setOrden(orden);
        pago.setMonto(12500);
        pago.setEstado(false);
        envio = new Envio();
        envio.setOrden(orden);
        envio.setEstado("Pendiente");

        when(pagoRepository.findByOrdenIdOrden(42)).thenReturn(List.of(pago));
        when(envioRepository.findByOrdenIdOrden(42)).thenReturn(Optional.of(envio));
    }

    private Map<String, Object> pagoMp(String estado, Object monto, String moneda, String referencia) {
        Map<String, Object> m = new HashMap<>();
        m.put("status", estado);
        m.put("transaction_amount", monto);
        m.put("currency_id", moneda);
        m.put("external_reference", referencia);
        return m;
    }

    @Test
    void pagoAprobadoConfirmaElPedido() {
        when(cliente.obtenerPago("555")).thenReturn(pagoMp("approved", 12500.0, "ARS", "42"));

        ResultadoPago r = servicio.sincronizar("555", 7);

        assertTrue(r.confirmado());
        assertEquals(42, r.idOrden());
        assertTrue(pago.isEstado());
        assertEquals("555", pago.getMpPaymentId());
        verify(pagoRepository).save(pago);
    }

    @Test
    void sincronizarPedidoConfirmaElPagoAprobadoQueEncuentraEnMercadoPago() {
        Map<String, Object> rechazado = new HashMap<>(Map.of("id", 111L, "status", "rejected"));
        Map<String, Object> aprobado = new HashMap<>(Map.of("id", 555L, "status", "approved"));
        when(cliente.buscarPagosPorReferencia(42)).thenReturn(List.of(rechazado, aprobado));
        when(cliente.obtenerPago("555")).thenReturn(pagoMp("approved", 12500, "ARS", "42"));

        ResultadoPago r = servicio.sincronizarPedido(42, 7);

        assertTrue(r.confirmado());
        assertTrue(pago.isEstado());
        verify(cliente, never()).obtenerPago("111");
    }

    @Test
    void sincronizarPedidoSinPagosAprobadosLoDejaPendiente() {
        when(cliente.buscarPagosPorReferencia(42)).thenReturn(List.of(new HashMap<>(Map.of("id", 111L, "status", "pending"))));

        ResultadoPago r = servicio.sincronizarPedido(42, 7);

        assertFalse(r.confirmado());
        assertFalse(pago.isEstado());
    }

    @Test
    void sincronizarPedidoDeOtroClienteSeRechazaSinConsultarAMercadoPago() {
        assertThrows(AccessDeniedException.class, () -> servicio.sincronizarPedido(42, 99));
        verify(cliente, never()).buscarPagosPorReferencia(anyInt());
    }

    @Test
    void elAvisoDelWebhookNoRestringeCliente() {
        when(cliente.obtenerPago("555")).thenReturn(pagoMp("approved", 12500, "ARS", "42"));

        assertTrue(servicio.sincronizar("555", null).confirmado());
    }

    @Test
    void pagoDeOtroClienteSeRechaza() {
        when(cliente.obtenerPago("555")).thenReturn(pagoMp("approved", 12500, "ARS", "42"));

        assertThrows(AccessDeniedException.class, () -> servicio.sincronizar("555", 99));
        assertFalse(pago.isEstado());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void montoDistintoNoConfirma() {
        when(cliente.obtenerPago("555")).thenReturn(pagoMp("approved", 100, "ARS", "42"));

        assertFalse(servicio.sincronizar("555", 7).confirmado());
        assertFalse(pago.isEstado());
    }

    @Test
    void monedaDistintaNoConfirma() {
        when(cliente.obtenerPago("555")).thenReturn(pagoMp("approved", 12500, "USD", "42"));

        assertFalse(servicio.sincronizar("555", 7).confirmado());
        assertFalse(pago.isEstado());
    }

    @Test
    void pagoPendienteORechazadoNoConfirma() {
        for (String estado : List.of("pending", "in_process", "rejected", "cancelled")) {
            when(cliente.obtenerPago("555")).thenReturn(pagoMp(estado, 12500, "ARS", "42"));
            ResultadoPago r = servicio.sincronizar("555", 7);
            assertEquals(estado, r.estadoMp());
            assertFalse(r.confirmado());
        }
        assertFalse(pago.isEstado());
    }

    @Test
    void pedidoCanceladoNoSeConfirmaAunqueLlegueElPago() {
        envio.setEstado("Cancelado");
        when(cliente.obtenerPago("555")).thenReturn(pagoMp("approved", 12500, "ARS", "42"));

        assertFalse(servicio.sincronizar("555", 7).confirmado());
        assertFalse(pago.isEstado());
    }

    @Test
    void confirmarDosVecesNoDuplicaNada() {
        when(cliente.obtenerPago("555")).thenReturn(pagoMp("approved", 12500, "ARS", "42"));

        servicio.sincronizar("555", 7);
        servicio.sincronizar("555", 7);

        verify(pagoRepository, times(1)).save(pago);
    }

    @Test
    void pagoInexistenteOSinReferenciaValidaDevuelveNull() {
        when(cliente.obtenerPago("1")).thenReturn(null);
        assertNull(servicio.sincronizar("1", 7));

        when(cliente.obtenerPago("2")).thenReturn(pagoMp("approved", 12500, "ARS", "no-es-numero"));
        assertNull(servicio.sincronizar("2", 7));

        when(cliente.obtenerPago("3")).thenReturn(pagoMp("approved", 12500, "ARS", "999"));
        when(pagoRepository.findByOrdenIdOrden(999)).thenReturn(List.of());
        assertNull(servicio.sincronizar("3", 7));
    }

    @Test
    @SuppressWarnings("unchecked")
    void laPreferenciaUsaLosPreciosDeLaBaseYLaReferenciaDelPedido() {
        Producto p = new Producto();
        p.setIdProducto(5);
        p.setNombre("Detergente");
        Detalle_Orden d = new Detalle_Orden();
        d.setProducto(p);
        d.setCantidad(2);
        d.setPrecioUnitario(6250);
        when(cliente.crearPreferencia(any())).thenReturn(Map.of("init_point", "https://mp.test/pagar"));

        String url = servicio.crearLinkDePago(pago.getOrden(), List.of(d));

        assertEquals("https://mp.test/pagar", url);
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(cliente).crearPreferencia(captor.capture());
        Map<String, Object> cuerpo = captor.getValue();
        assertEquals("42", cuerpo.get("external_reference"));
        Map<String, Object> item = ((List<Map<String, Object>>) cuerpo.get("items")).get(0);
        assertEquals(6250, item.get("unit_price"));
        assertEquals(2, item.get("quantity"));
        assertEquals("ARS", item.get("currency_id"));
        Map<String, String> back = (Map<String, String>) cuerpo.get("back_urls");
        assertEquals("http://localhost:3000/pago/resultado", back.get("success"));
        assertFalse(cuerpo.containsKey("auto_return"), "auto_return no funciona con http");
        assertFalse(cuerpo.containsKey("notification_url"), "sin webhook configurado no se manda");
    }

    @Test
    void sinLinkEnLaRespuestaFalla() {
        when(cliente.crearPreferencia(any())).thenReturn(Map.of());

        assertThrows(MercadoPagoException.class, () -> servicio.crearLinkDePago(pago.getOrden(), List.of()));
    }

    @Test
    void pedidosSinPagarVencidosSeCancelanYDevuelvenStock() {
        when(envioRepository.findPendientesSinPagarAntesDe(any())).thenReturn(List.of(envio));

        servicio.cancelarSinPagar();

        assertEquals("Cancelado", envio.getEstado());
        verify(cancelacion).devolverStock(42);
        verify(envioRepository).save(envio);
    }
}
