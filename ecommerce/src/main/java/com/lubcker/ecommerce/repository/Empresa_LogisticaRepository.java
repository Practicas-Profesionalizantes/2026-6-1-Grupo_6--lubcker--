package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Empresa_Logistica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Empresa_LogisticaRepository extends JpaRepository<Empresa_Logistica, Integer> {
    // ¡Listo! No necesitas escribir nada más aquí.
    // Spring ya te da métodos como: save(), findById(), findAll(), deleteById(), etc.
}