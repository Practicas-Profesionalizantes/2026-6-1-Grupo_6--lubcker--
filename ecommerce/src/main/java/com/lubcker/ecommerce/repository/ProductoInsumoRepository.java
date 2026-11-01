package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.ProductoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoInsumoRepository extends JpaRepository<ProductoInsumo, Integer> {

    @Query("select pi from ProductoInsumo pi join fetch pi.insumo where pi.producto.idProducto = :idProducto order by pi.insumo.nombre")
    List<ProductoInsumo> findByProducto(@Param("idProducto") int idProducto);

    // Todas las recetas de una vez (para el resumen de costos de todos los productos)
    @Query("select pi from ProductoInsumo pi join fetch pi.insumo join fetch pi.producto")
    List<ProductoInsumo> findAllConDetalle();

    // Cuantos productos usan un insumo
    long countByInsumoIdInsumo(int idInsumo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProductoInsumo pi where pi.producto.idProducto = :idProducto")
    void borrarPorProducto(@Param("idProducto") int idProducto);
}
