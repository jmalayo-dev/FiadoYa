package pe.edu.cibertec.fiadoya.common.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Piezas JWT compartidas: clave HS256, decodificador con validación de issuer/exp y conversor de roles. */
public final class JwtSoporte {

    private JwtSoporte() {
    }

    public static SecretKey clave(String secreto) {
        if (secreto == null || secreto.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public static JwtDecoder decodificador(SeguridadProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave(props.jwtSecret()))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.jwtIssuer())));
        return decoder;
    }

    /** rol=BODEGUERO → ROLE_BODEGUERO; el nombre de la autenticación es el sub (id de usuario). */
    public static Converter<Jwt, AbstractAuthenticationToken> conversorRoles() {
        return jwt -> {
            String rol = jwt.getClaimAsString(Roles.CLAIM_ROL);
            var autoridades = rol == null ? List.<SimpleGrantedAuthority>of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
            return new JwtAuthenticationToken(jwt, autoridades, jwt.getSubject());
        };
    }
}
