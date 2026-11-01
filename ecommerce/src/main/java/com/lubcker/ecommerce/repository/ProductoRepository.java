package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByActivoTrue();

    List<Producto> findByCategoriaIdCategoriaAndActivoTrue(int idCategoria);

    boolean existsByCategoriaIdCategoria(int idCategoria);

    // Bloquea la fila hasta que termine la transaccion: dos compras simultaneas del
    // ultimo producto no pueden descontar el mismo stock dos veces.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.idProducto = :id")
    Optional<Producto> findByIdParaActualizar(@Param("id") int id);
}
