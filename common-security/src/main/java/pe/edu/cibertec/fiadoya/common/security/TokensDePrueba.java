package pe.edu.cibertec.fiadoya.common.security;

import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Firma tokens con el secreto dado. Solo para pruebas de integración de los servicios. */
public final class TokensDePrueba {

    private TokensDePrueba() {
    }

    public static String token(String secreto, long usuarioId, String rol) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("fiadoya-auth").subject(String.valueOf(usuarioId))
                .issuedAt(ahora).expiresAt(ahora.plusSeconds(3600)).claim("username", "prueba").claim(Roles.CLAIM_ROL, rol)
                .build();
        return NimbusJwtEncoder.withSecretKey(JwtSoporte.clave(secreto)).build()
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public static String bearer(String secreto, long usuarioId, String rol) {
        return "Bearer " + token(secreto, usuarioId, rol);
    }
}
