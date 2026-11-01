package com.lubcker.ecommerce.service;

import com.lubcker.ecommerce.mercadopago.OrdenCancelacionService;
import com.lubcker.ecommerce.model.Envio;
import com.lubcker.ecommerce.repository.Detalle_OrdenRepository;
import com.lubcker.ecommerce.repository.EnvioRepository;
import com.lubcker.ecommerce.repository.OrdenRepository;
import com.lubcker.ecommerce.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Borra un pedido con sus detalles, pagos y envio (todo o nada).
// Si el pedido todavia no salio (envio Pendiente o En preparación), las unidades vuelven al stock;
// si ya fue enviado/entregado no se toca el stock, y si estaba cancelado ya se habia devuelto.
@Service
public class OrdenEliminacionService {

    private final OrdenRepository ordenRepository;
    private final Detalle_OrdenRepository detalleOrdenRepository;
    private final PagoRepository pagoRepository;
    private final EnvioRepository envioRepository;
    private final OrdenCancelacionService cancelacion;

    public OrdenEliminacionService(OrdenRepository ordenRepository, Detalle_OrdenRepository detalleOrdenRepository,
                                   PagoRepository pagoRepository, EnvioRepository envioRepository,
                                   OrdenCancelacionService cancelacion) {
        this.ordenRepository = ordenRepository;
        this.detalleOrdenRepository = detalleOrdenRepository;
        this.pagoRepository = pagoRepository;
        this.envioRepository = envioRepository;
        this.cancelacion = cancelacion;
    }

    // Devuelve false si el pedido no existe
    @Transactional
    public boolean eliminar(int idOrden) {
        if (!ordenRepository.existsById(idOrden)) return false;

        Optional<Envio> envio = envioRepository.findByOrdenIdOrden(idOrden);
        String estado = envio.map(e -> e.getEstado() == null ? "" : e.getEstado().toLowerCase()).orElse("pendiente");
        if (estado.equals("pendiente") || estado.equals("en preparación")) {
            cancelacion.devolverStock(idOrden);
        }

        // Borrados directos (sin cargar entidades): evita que Hibernate se queje de que la orden
        // todavia apunta al envio/pagos recien borrados
        pagoRepository.borrarPorOrden(idOrden);
        envioRepository.borrarPorOrden(idOrden);
        detalleOrdenRepository.borrarPorOrden(idOrden);
        ordenRepository.borrarPorId(idOrden);
        return true;
    }
}
