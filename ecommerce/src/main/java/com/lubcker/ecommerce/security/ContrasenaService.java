package com.lubcker.ecommerce.security;

import com.lubcker.ecommerce.model.Usuario;
import com.lubcker.ecommerce.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Hash de contrasenas (BCrypt). Las contrasenas que quedaron guardadas en texto plano de
// antes se aceptan una vez y se convierten a hash en ese mismo momento.
@Service
public class ContrasenaService {

    private final PasswordEncoder encoder;
    private final UsuarioRepository usuarioRepository;

    public ContrasenaService(PasswordEncoder encoder, UsuarioRepository usuarioRepository) {
        this.encoder = encoder;
        this.usuarioRepository = usuarioRepository;
    }

    public String hashear(String contrasenaPlana) {
        return encoder.encode(contrasenaPlana);
    }

    public boolean verificar(Usuario usuario, String contrasenaPlana) {
        String guardada = usuario.getContrasena();
        if (guardada == null || contrasenaPlana == null) return false;

        if (esHashBcrypt(guardada)) {
            return encoder.matches(contrasenaPlana, guardada);
        }

        // Contrasena vieja en texto plano: si coincide, se migra a hash ahora.
        if (guardada.equals(contrasenaPlana)) {
            usuario.setContrasena(hashear(contrasenaPlana));
            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }

    private boolean esHashBcrypt(String valor) {
        return valor.startsWith("$2a$") || valor.startsWith("$2b$") || valor.startsWith("$2y$");
    }
}
