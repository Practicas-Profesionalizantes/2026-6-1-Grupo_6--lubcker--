package com.lubcker.ecommerce.mercadopago;

import com.lubcker.ecommerce.model.Detalle_Orden;
import com.lubcker.ecommerce.model.Producto;
import com.lubcker.ecommerce.repository.Detalle_OrdenRepository;
import com.lubcker.ecommerce.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Devuelve al stock las unidades de un pedido que se cancela.
@Service
public class OrdenCancelacionService {

    private final Detalle_OrdenRepository detalleOrdenRepository;
    private final ProductoRepository productoRepository;

    public OrdenCancelacionService(Detalle_OrdenRepository detalleOrdenRepository, ProductoRepository productoRepository) {
        this.detalleOrdenRepository = detalleOrdenRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public void devolverStock(int idOrden) {
        for (Detalle_Orden detalle : detalleOrdenRepository.findByOrdenIdOrden(idOrden)) {
            Producto producto = productoRepository.findByIdParaActualizar(detalle.getProducto().getIdProducto())
                    .orElse(null);
            if (producto != null) {
                producto.setStock(producto.getStock() + detalle.getCantidad() * detalle.getTamano());
                productoRepository.save(producto);
            }
        }
    }
}
