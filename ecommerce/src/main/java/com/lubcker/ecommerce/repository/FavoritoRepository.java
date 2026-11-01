package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Integer> {

    List<Favorito> findByClienteIdCliente(int idCliente);

    Optional<Favorito> findByClienteIdClienteAndProductoIdProducto(int idCliente, int idProducto);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Favorito f where f.producto.idProducto = :idProducto")
    void borrarPorProducto(@Param("idProducto") int idProducto);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Favorito f where f.cliente.idCliente = :idCliente")
    void borrarPorCliente(@Param("idCliente") int idCliente);
}
