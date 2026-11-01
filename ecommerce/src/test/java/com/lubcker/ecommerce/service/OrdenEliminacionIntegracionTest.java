package com.lubcker.ecommerce.service;

import com.lubcker.ecommerce.model.*;
import com.lubcker.ecommerce.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// Contra la base real, pero todo ocurre dentro de una transaccion que se deshace al terminar
// (@Transactional en un test hace rollback): no queda ningun dato.
@SpringBootTest
@Transactional
class OrdenEliminacionIntegracionTest {

    @Autowired private OrdenEliminacionService servicio;
    @Autowired private OrdenRepository ordenRepository;
    @Autowired private Detalle_OrdenRepository detalleRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private EnvioRepository envioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private DireccionRepository direccionRepository;
    @Autowired private Empresa_LogisticaRepository empresaRepository;
    @Autowired private EntityManager em;

    @Test
    void eliminaElPedidoConSusDetallesPagoYEnvioYDevuelveElStock() {
        Cliente cliente = clienteRepository.findAll().stream().findFirst().orElse(null);
        Producto producto = productoRepository.findAll().stream().findFirst().orElse(null);
        Direccion direccion = direccionRepository.findAll().stream().findFirst().orElse(null);
        Empresa_Logistica empresa = empresaRepository.findAll().stream().findFirst().orElse(null);
        assumeFalse(cliente == null || producto == null || direccion == null || empresa == null,
                "La base no tiene datos de prueba");

        int stockAntes = producto.getStock();

        Orden orden = new Orden();
        orden.setCliente(cliente);
        orden.setFecha(new Date());
        orden = ordenRepository.save(orden);
        int idOrden = orden.getIdOrden();

        Detalle_Orden detalle = new Detalle_Orden();
        detalle.setOrden(orden);
        detalle.setProducto(producto);
        detalle.setCantidad(2);
        detalle.setTamano(1);
        detalle.setSubtotal(100);
        detalle.setPrecioUnitario(50);
        detalleRepository.save(detalle);

        Pago pago = new Pago();
        pago.setOrden(orden);
        pago.setMonto(100);
        pagoRepository.save(pago);

        Envio envio = new Envio();
        envio.setOrden(orden);
        envio.setEstado("Pendiente");
        envio.setDireccion(direccion);
        envio.setEmpresaLogistica(empresa);
        envioRepository.save(envio);
        em.flush();
        em.clear();

        assertTrue(servicio.eliminar(idOrden));

        assertFalse(ordenRepository.existsById(idOrden));
        assertTrue(detalleRepository.findByOrdenIdOrden(idOrden).isEmpty());
        assertTrue(pagoRepository.findByOrdenIdOrden(idOrden).isEmpty());
        assertTrue(envioRepository.findByOrdenIdOrden(idOrden).isEmpty());
        // Pedido pendiente: las 2 unidades vuelven al stock
        assertEquals(stockAntes + 2, productoRepository.findById(producto.getIdProducto()).orElseThrow().getStock());
    }
}
