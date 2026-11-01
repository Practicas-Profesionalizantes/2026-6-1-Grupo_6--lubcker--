package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Cliente c where c.idCliente = :idCliente")
    void borrarPorId(@Param("idCliente") int idCliente);
}
