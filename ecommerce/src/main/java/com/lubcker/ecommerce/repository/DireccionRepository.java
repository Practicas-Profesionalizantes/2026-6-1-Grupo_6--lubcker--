package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Integer> {

    List<Direccion> findByClienteIdCliente(int idCliente);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Direccion d where d.cliente.idCliente = :idCliente")
    void borrarPorCliente(@Param("idCliente") int idCliente);
}
