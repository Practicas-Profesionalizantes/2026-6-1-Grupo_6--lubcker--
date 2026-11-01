package com.lubcker.ecommerce.security;

import com.lubcker.ecommerce.model.Usuario;
import com.lubcker.ecommerce.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContrasenaServiceTest {

    private UsuarioRepository repositorio;
    private ContrasenaService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        servicio = new ContrasenaService(new BCryptPasswordEncoder(), repositorio);
    }

    private Usuario usuarioCon(String contrasenaGuardada) {
        Usuario u = new Usuario();
        u.setContrasena(contrasenaGuardada);
        return u;
    }

    @Test
    void elHashNoEsLaContrasenaYSeVerifica() {
        String hash = servicio.hashear("clave1234");

        assertNotEquals("clave1234", hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(servicio.verificar(usuarioCon(hash), "clave1234"));
        assertFalse(servicio.verificar(usuarioCon(hash), "otra1234"));
    }

    @Test
    void unaContrasenaViejaEnTextoPlanoSeAceptaYSeMigraAHash() {
        Usuario viejo = usuarioCon("legacy1234");

        assertTrue(servicio.verificar(viejo, "legacy1234"));
        assertTrue(viejo.getContrasena().startsWith("$2"), "debería haberse guardado con hash");
        verify(repositorio).save(viejo);
    }

    @Test
    void unaContrasenaViejaIncorrectaNoSeMigra() {
        Usuario viejo = usuarioCon("legacy1234");

        assertFalse(servicio.verificar(viejo, "incorrecta"));
        assertEquals("legacy1234", viejo.getContrasena());
        verify(repositorio, never()).save(any());
    }

    @Test
    void valoresNulosNoPasan() {
        assertFalse(servicio.verificar(usuarioCon(null), "x"));
        assertFalse(servicio.verificar(usuarioCon(servicio.hashear("x1234567")), null));
    }
}
