package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Detalle_Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Detalle_OrdenRepository extends JpaRepository<Detalle_Orden, Integer> {

    List<Detalle_Orden> findByOrdenIdOrden(int idOrden);

    boolean existsByProductoIdProducto(int idProducto);

    // ¿El cliente compro este producto? Cuentan solo los pedidos ya pagados y sin envio cancelado.
    @Query("select count(d) > 0 from Detalle_Orden d "
            + "where d.orden.cliente.idCliente = :idCliente and d.producto.idProducto = :idProducto "
            + "and exists (select p from Pago p where p.orden = d.orden and p.estado = true) "
            + "and not exists (select e from Envio e where e.orden = d.orden and lower(e.estado) = 'cancelado')")
    boolean clienteComproProducto(@Param("idCliente") int idCliente, @Param("idProducto") int idProducto);

    // Total comprado por un cliente: solo pedidos pagados y sin cancelar
    @Query("select coalesce(sum(d.subtotal), 0) from Detalle_Orden d where d.orden.cliente.idCliente = :idCliente "
            + "and exists (select p from Pago p where p.orden = d.orden and p.estado = true) "
            + "and not exists (select e from Envio e where e.orden = d.orden and lower(e.estado) = 'cancelado')")
    long totalGastado(@Param("idCliente") int idCliente);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Detalle_Orden d where d.orden.idOrden = :idOrden")
    void borrarPorOrden(@Param("idOrden") int idOrden);
}
