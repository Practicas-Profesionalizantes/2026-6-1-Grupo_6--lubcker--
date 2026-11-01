package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Resenas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResenasRepository extends JpaRepository<Resenas, Integer> {

    List<Resenas> findByProductoIdProductoOrderByIdResenasDesc(int idProducto);

    List<Resenas> findByClienteIdCliente(int idCliente);

    Optional<Resenas> findByClienteIdClienteAndProductoIdProducto(int idCliente, int idProducto);

    // Para la pagina principal: las mejor valoradas, las mas nuevas primero
    List<Resenas> findTop6ByValoracionGreaterThanEqualOrderByIdResenasDesc(int valoracionMinima);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Resenas r where r.producto.idProducto = :idProducto")
    void borrarPorProducto(@Param("idProducto") int idProducto);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Resenas r where r.cliente.idCliente = :idCliente")
    void borrarPorCliente(@Param("idCliente") int idCliente);
}
