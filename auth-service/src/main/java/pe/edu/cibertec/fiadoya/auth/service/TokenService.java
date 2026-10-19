package pe.edu.cibertec.fiadoya.auth.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import pe.edu.cibertec.fiadoya.auth.config.AuthProperties;
import pe.edu.cibertec.fiadoya.auth.entity.Usuario;
import pe.edu.cibertec.fiadoya.common.security.Roles;
import pe.edu.cibertec.fiadoya.common.security.SeguridadProperties;

/** Emite el JWT con los claims del Anexo §3.1: iss, sub, username, rol, iat, exp. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration vigencia;

    public TokenService(JwtEncoder encoder, SeguridadProperties seguridad, AuthProperties props) {
        this.encoder = encoder;
        this.issuer = seguridad.jwtIssuer();
        this.vigencia = Duration.ofHours(props.jwt().ttlHoras() > 0 ? props.jwt().ttlHoras() : 8);
    }

    public Emitido emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(vigencia);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim("username", usuario.getUsername())
                .claim(Roles.CLAIM_ROL, usuario.getRol().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Emitido(token, expira);
    }

    public record Emitido(String token, Instant expiraEn) {
    }
}
