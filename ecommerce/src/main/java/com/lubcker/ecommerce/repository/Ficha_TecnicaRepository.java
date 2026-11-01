package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Ficha_Tecnica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Ficha_TecnicaRepository extends JpaRepository<Ficha_Tecnica, Integer> {

    Optional<Ficha_Tecnica> findByProductoIdProducto(int idProducto);

    // Borrado directo en la base (sin cargar entidades) para poder eliminar el producto despues
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Ficha_Tecnica f where f.producto.idProducto = :idProducto")
    void borrarPorProducto(@Param("idProducto") int idProducto);
}
