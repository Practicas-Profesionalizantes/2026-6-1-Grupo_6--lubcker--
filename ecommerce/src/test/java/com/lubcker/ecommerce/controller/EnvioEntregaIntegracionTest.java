package com.lubcker.ecommerce.controller;

import com.lubcker.ecommerce.dto.EntregaRequestDTO;
import com.lubcker.ecommerce.dto.EnvioDTO;
import com.lubcker.ecommerce.model.*;
import com.lubcker.ecommerce.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// Contra la base real, pero dentro de una transaccion que se deshace al terminar (no queda ningun dato).
@SpringBootTest
@Transactional
class EnvioEntregaIntegracionTest {

    @Autowired private EnvioController controller;
    @Autowired private OrdenRepository ordenRepository;
    @Autowired private EnvioRepository envioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private DireccionRepository direccionRepository;
    @Autowired private Empresa_LogisticaRepository empresaRepository;

    private Envio nuevoEnvio(String estado) {
        Cliente cliente = clienteRepository.findAll().stream().findFirst().orElse(null);
        Direccion direccion = direccionRepository.findAll().stream().findFirst().orElse(null);
        Empresa_Logistica empresa = empresaRepository.findAll().stream().findFirst().orElse(null);
        assumeFalse(cliente == null || direccion == null || empresa == null, "La base no tiene datos de prueba");

        Orden orden = new Orden();
        orden.setCliente(cliente);
        orden.setFecha(new Date());
        orden = ordenRepository.save(orden);

        Envio envio = new Envio();
        envio.setOrden(orden);
        envio.setEstado(estado);
        envio.setDireccion(direccion);
        envio.setEmpresaLogistica(empresa);
        return envioRepository.save(envio);
    }

    @Test
    void guardaLaFechaLaFranjaYElMensajeParaElCliente() {
        Envio envio = nuevoEnvio("En preparación");
        String manana = LocalDate.now().plusDays(1).toString();

        ResponseEntity<?> r = controller.programarEntrega(envio.getIdEnvio(),
                new EntregaRequestDTO(manana, "de 14 a 16 hs", "Te llamo al llegar"));

        assertEquals(HttpStatus.OK, r.getStatusCode());
        EnvioDTO dto = (EnvioDTO) r.getBody();
        assertEquals(manana, dto.getFechaEntrega());
        assertEquals("de 14 a 16 hs", dto.getFranjaHoraria());
        assertEquals("Te llamo al llegar", dto.getNotaCliente());
        assertEquals(LocalDate.now().plusDays(1), envioRepository.findById(envio.getIdEnvio()).orElseThrow().getFechaEntrega());
    }

    @Test
    void sinFechaYConTextosVaciosLimpiaLaEntrega() {
        Envio envio = nuevoEnvio("Pendiente");
        controller.programarEntrega(envio.getIdEnvio(),
                new EntregaRequestDTO(LocalDate.now().plusDays(2).toString(), "mañana", "hola"));

        ResponseEntity<?> r = controller.programarEntrega(envio.getIdEnvio(), new EntregaRequestDTO("", " ", ""));

        EnvioDTO dto = (EnvioDTO) r.getBody();
        assertNull(dto.getFechaEntrega());
        assertNull(dto.getFranjaHoraria());
        assertNull(dto.getNotaCliente());
    }

    @Test
    void rechazaUnaFechaPasadaOInvalida() {
        Envio envio = nuevoEnvio("Pendiente");

        ResponseEntity<?> pasada = controller.programarEntrega(envio.getIdEnvio(),
                new EntregaRequestDTO(LocalDate.now().minusDays(1).toString(), null, null));
        ResponseEntity<?> invalida = controller.programarEntrega(envio.getIdEnvio(),
                new EntregaRequestDTO("31/12/2026", null, null));

        assertEquals(HttpStatus.BAD_REQUEST, pasada.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, invalida.getStatusCode());
        assertNull(envioRepository.findById(envio.getIdEnvio()).orElseThrow().getFechaEntrega());
    }

    @Test
    void unPedidoCanceladoNoSePuedeProgramar() {
        Envio envio = nuevoEnvio("Cancelado");

        ResponseEntity<?> r = controller.programarEntrega(envio.getIdEnvio(),
                new EntregaRequestDTO(LocalDate.now().plusDays(1).toString(), null, null));

        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
    }

    @Test
    void unEnvioInexistenteDaNotFound() {
        assertEquals(HttpStatus.NOT_FOUND, controller.programarEntrega(-1, new EntregaRequestDTO(null, null, null)).getStatusCode());
    }
}
