package pe.edu.cibertec.fiadoya.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * fiadoya.security.* — valores que llegan por variables de entorno (JWT_SECRET, JWT_ISSUER, INTERNAL_API_KEY).
 *
 * @param jwtSecret   secreto HS256 de al menos 32 caracteres
 * @param jwtIssuer   emisor esperado (fiadoya-auth)
 * @param internalKey clave compartida para /internal/**
 */
@ConfigurationProperties(prefix = "fiadoya.security")
public record SeguridadProperties(String jwtSecret, String jwtIssuer, String internalKey) {

    public SeguridadProperties {
        if (jwtIssuer == null || jwtIssuer.isBlank()) {
            jwtIssuer = "fiadoya-auth";
        }
    }
}
