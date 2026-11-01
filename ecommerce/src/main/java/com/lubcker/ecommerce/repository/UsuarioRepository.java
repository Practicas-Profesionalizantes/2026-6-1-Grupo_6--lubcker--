package com.lubcker.ecommerce.repository;

import com.lubcker.ecommerce.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    // Borrado directo (sin cargar la relacion Cliente <-> Usuario, que Hibernate no deja borrar)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Usuario u where u.idUsuario = :idUsuario")
    void borrarPorId(@Param("idUsuario") int idUsuario);
}
