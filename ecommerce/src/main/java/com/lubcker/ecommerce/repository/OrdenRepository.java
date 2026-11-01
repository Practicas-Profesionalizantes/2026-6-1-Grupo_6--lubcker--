package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Integer> {

    long countByClienteIdCliente(int idCliente);

    // Pedidos que el cliente ya pago (los demas no son compras: carritos abandonados, pagos sin completar)
    @Query("select count(o) from Orden o where o.cliente.idCliente = :idCliente "
            + "and exists (select p from Pago p where p.orden = o and p.estado = true)")
    long contarPagados(@Param("idCliente") int idCliente);

    @Query("select o.idOrden from Orden o where o.cliente.idCliente = :idCliente "
            + "and not exists (select p from Pago p where p.orden = o and p.estado = true)")
    List<Integer> idsSinPagar(@Param("idCliente") int idCliente);

    // Pedidos de UN cliente, el mas nuevo primero (lo usa el asistente de chat)
    List<Orden> findByClienteIdClienteOrderByIdOrdenDesc(int idCliente);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Orden o where o.idOrden = :idOrden")
    void borrarPorId(@Param("idOrden") int idOrden);
}
