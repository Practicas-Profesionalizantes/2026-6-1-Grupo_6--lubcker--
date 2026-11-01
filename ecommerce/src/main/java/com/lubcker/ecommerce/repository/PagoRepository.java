package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {
    // ¡Listo! No necesitas escribir nada más aquí.
    // Spring ya te da métodos como: save(), findById(), findAll(), deleteById(), etc.

    List<Pago> findByOrdenClienteIdCliente(int idCliente);

    List<Pago> findByOrdenIdOrden(int idOrden);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Pago p where p.orden.idOrden = :idOrden")
    void borrarPorOrden(@Param("idOrden") int idOrden);
}
