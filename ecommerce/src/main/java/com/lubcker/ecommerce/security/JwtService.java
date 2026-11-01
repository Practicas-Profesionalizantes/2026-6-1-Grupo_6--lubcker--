package com.lubcker.ecommerce.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

// Crea y valida los tokens de sesion (JWT firmados con HMAC-SHA256).
// El token solo lleva el id del usuario; el rol se consulta en la base en cada pedido,
// asi un cambio de rol o un usuario borrado surten efecto de inmediato.
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final byte[] secreto;
    private final long horasValidez;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.horas-validez:8}") long horasValidez) {
        this.horasValidez = horasValidez;

        if (secret == null || secret.isBlank()) {
            // Sin clave configurada se genera una al azar: sirve para desarrollar, pero
            // todas las sesiones se pierden cada vez que se reinicia el backend.
            this.secreto = new byte[32];
            new SecureRandom().nextBytes(this.secreto);
            log.warn("JWT_SECRET no esta configurada: se usa una clave aleatoria temporal. "
                    + "Configurala (minimo 32 caracteres) para que las sesiones sobrevivan a reinicios.");
        } else {
            byte[] bytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres.");
            }
            this.secreto = bytes;
        }
    }

    public String generar(int idUsuario) {
        try {
            Instant ahora = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(idUsuario))
                    .issueTime(Date.from(ahora))
                    .expirationTime(Date.from(ahora.plusSeconds(horasValidez * 3600)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secreto));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }

    // Devuelve el id del usuario si el token es valido (firma correcta y no vencido).
    public Optional<Integer> validar(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) return Optional.empty();
            if (!jwt.verify(new MACVerifier(secreto))) return Optional.empty();

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date vence = claims.getExpirationTime();
            if (vence == null || vence.before(new Date())) return Optional.empty();

            return Optional.of(Integer.parseInt(claims.getSubject()));
        } catch (ParseException | JOSEException | NumberFormatException e) {
            return Optional.empty();
        }
    }
}
