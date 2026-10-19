package pe.edu.cibertec.fiadoya.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import pe.edu.cibertec.fiadoya.common.security.JwtSoporte;
import pe.edu.cibertec.fiadoya.common.security.SeguridadProperties;

@Configuration
public class AuthConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Solo auth-service firma tokens (HS256 con JWT_SECRET). */
    @Bean
    JwtEncoder jwtEncoder(SeguridadProperties props) {
        return NimbusJwtEncoder.withSecretKey(JwtSoporte.clave(props.jwtSecret())).build();
    }
}
