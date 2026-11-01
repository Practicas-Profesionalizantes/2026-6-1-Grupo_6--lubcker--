package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.ResenaGeneral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResenaGeneralRepository extends JpaRepository<ResenaGeneral, Integer> {

    List<ResenaGeneral> findAllByOrderByIdResenaGeneralDesc();

    Optional<ResenaGeneral> findByClienteIdCliente(int idCliente);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ResenaGeneral r where r.cliente.idCliente = :idCliente")
    void borrarPorCliente(@Param("idCliente") int idCliente);
}
