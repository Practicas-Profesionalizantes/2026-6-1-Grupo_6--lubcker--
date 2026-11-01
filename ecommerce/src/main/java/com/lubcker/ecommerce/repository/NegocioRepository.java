package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NegocioRepository extends JpaRepository<Negocio, Integer> {

    Optional<Negocio> findByClienteIdCliente(int idCliente);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Negocio n where n.cliente.idCliente = :idCliente")
    void borrarPorCliente(@Param("idCliente") int idCliente);
}
