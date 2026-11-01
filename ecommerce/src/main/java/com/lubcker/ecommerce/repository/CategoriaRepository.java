package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    // ¡Listo! No necesitas escribir nada más aquí.
    // Spring ya te da métodos como: save(), findById(), findAll(), deleteById(), etc.
}