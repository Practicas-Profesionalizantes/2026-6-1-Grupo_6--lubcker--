package com.lubcker.ecommerce.service;

import com.lubcker.ecommerce.mercadopago.OrdenCancelacionService;
import com.lubcker.ecommerce.model.Envio;
import com.lubcker.ecommerce.repository.Detalle_OrdenRepository;
import com.lubcker.ecommerce.repository.EnvioRepository;
import com.lubcker.ecommerce.repository.OrdenRepository;
import com.lubcker.ecommerce.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrdenEliminacionServiceTest {

    private OrdenRepository ordenRepository;
    private Detalle_OrdenRepository detalleRepository;
    private PagoRepository pagoRepository;
    private EnvioRepository envioRepository;
    private OrdenCancelacionService cancelacion;
    private OrdenEliminacionService servicio;

    @BeforeEach
    void preparar() {
        ordenRepository = mock(OrdenRepository.class);
        detalleRepository = mock(Detalle_OrdenRepository.class);
        pagoRepository = mock(PagoRepository.class);
        envioRepository = mock(EnvioRepository.class);
        cancelacion = mock(OrdenCancelacionService.class);
        servicio = new OrdenEliminacionService(ordenRepository, detalleRepository, pagoRepository, envioRepository, cancelacion);

        when(ordenRepository.existsById(5)).thenReturn(true);
    }

    private void conEnvio(String estado) {
        Envio envio = new Envio();
        envio.setEstado(estado);
        when(envioRepository.findByOrdenIdOrden(5)).thenReturn(Optional.of(envio));
    }

    @Test
    void pedidoInexistenteDevuelveFalseYNoBorraNada() {
        assertFalse(servicio.eliminar(99));
        verify(ordenRepository, never()).borrarPorId(anyInt());
        verifyNoInteractions(cancelacion);
    }

    @Test
    void pedidoPendienteDevuelveElStockYBorraTodo() {
        conEnvio("Pendiente");
        assertTrue(servicio.eliminar(5));
        verify(cancelacion).devolverStock(5);
        verify(envioRepository).borrarPorOrden(5);
        verify(ordenRepository).borrarPorId(5);
    }

    @Test
    void pedidoEnPreparacionDevuelveElStock() {
        conEnvio("En preparación");
        servicio.eliminar(5);
        verify(cancelacion).devolverStock(5);
    }

    @Test
    void pedidoEnviadoOCanceladoNoTocaElStock() {
        for (String estado : List.of("Enviado", "Entregado", "Cancelado")) {
            conEnvio(estado);
            servicio.eliminar(5);
        }
        verify(cancelacion, never()).devolverStock(anyInt());
        verify(ordenRepository, times(3)).borrarPorId(5);
    }
}
