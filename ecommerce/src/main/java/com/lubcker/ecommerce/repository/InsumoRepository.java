package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Integer> {

    List<Insumo> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdInsumoNot(String nombre, int idInsumo);
}
