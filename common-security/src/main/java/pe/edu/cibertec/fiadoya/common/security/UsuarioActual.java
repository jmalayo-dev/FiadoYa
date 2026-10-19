package pe.edu.cibertec.fiadoya.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Lee el id de usuario (claim sub) del JWT de la petición actual. */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static Long id(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt) {
            return Long.valueOf(jwt.getToken().getSubject());
        }
        throw new IllegalStateException("La petición no tiene un JWT");
    }
}
