package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, Integer> {
    // ¡Listo! No necesitas escribir nada más aquí.
    // Spring ya te da métodos como: save(), findById(), findAll(), deleteById(), etc.

    List<Envio> findByOrdenClienteIdCliente(int idCliente);

    Optional<Envio> findByOrdenIdOrden(int idOrden);

    // Pedidos con envio "Pendiente" creados antes del limite y sin ningun pago aprobado
    @Query("select e from Envio e where lower(e.estado) = 'pendiente' and e.orden.fecha < :limite "
            + "and exists (select p from Pago p where p.orden = e.orden) "
            + "and not exists (select p from Pago p where p.orden = e.orden and p.estado = true)")
    List<Envio> findPendientesSinPagarAntesDe(@Param("limite") Date limite);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Envio e where e.orden.idOrden = :idOrden")
    void borrarPorOrden(@Param("idOrden") int idOrden);
}
