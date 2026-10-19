package pe.edu.cibertec.fiadoya.gateway;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * Matriz de acceso del Anexo §3.2 aplicada en el gateway (además, cada servicio valida el JWT: R6).
 * Errores 401/403 con el mismo formato ProblemDetail que los servicios.
 */
@Configuration
@EnableWebFluxSecurity
public class SeguridadGateway {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Bean
    SecurityWebFilterChain cadena(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(c -> {
                })
                .authorizeExchange(a -> a
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/internal/**").denyAll()
                        .pathMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/webjars/**", "/v3/api-docs/**", "/docs/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .pathMatchers("/api/auth/me", "/api/bodega").authenticated()
                        .pathMatchers("/api/clientes/mi-cuenta").hasRole("CLIENTE")
                        .pathMatchers("/api/**").hasRole("BODEGUERO")
                        .anyExchange().denyAll())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(conversor()))
                        .authenticationEntryPoint((ex, e) -> escribir(ex.getResponse(), HttpStatus.UNAUTHORIZED,
                                "AUTH_TOKEN_EXPIRED", "Sesión vencida o token inválido", "Token ausente, vencido o inválido."))
                        .accessDeniedHandler((ex, e) -> escribir(ex.getResponse(), HttpStatus.FORBIDDEN,
                                "AUTH_FORBIDDEN", "Sin permiso", "No tienes permiso para este recurso.")))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((ex, err) -> escribir(ex.getResponse(), HttpStatus.UNAUTHORIZED,
                                "AUTH_TOKEN_EXPIRED", "Sesión vencida o token inválido", "Token ausente, vencido o inválido."))
                        .accessDeniedHandler((ex, err) -> escribir(ex.getResponse(), HttpStatus.FORBIDDEN,
                                "AUTH_FORBIDDEN", "Sin permiso", "No tienes permiso para este recurso.")))
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(@Value("${fiadoya.security.jwt-secret}") String secreto,
                                  @Value("${fiadoya.security.jwt-issuer:fiadoya-auth}") String issuer) {
        if (secreto == null || secreto.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withSecretKey(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer)));
        return decoder;
    }

    /** Solo en desarrollo con Vite hace falta CORS; en Docker todo pasa por Nginx (mismo origen). */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${fiadoya.cors-origins:http://localhost:5173}") String origenes) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "Accept"));
        cors.setExposedHeaders(List.of("X-Tiempo-Paralelo-Ms", "X-Tiempo-Secuencial-Ms", "Content-Disposition"));
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    private static Converter<Jwt, Mono<AbstractAuthenticationToken>> conversor() {
        return jwt -> {
            String rol = jwt.getClaimAsString("rol");
            var autoridades = rol == null ? List.<SimpleGrantedAuthority>of() : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
            return Mono.just(new JwtAuthenticationToken(jwt, autoridades, jwt.getSubject()));
        };
    }

    private static Mono<Void> escribir(ServerHttpResponse response, HttpStatus status, String code, String titulo, String detalle) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("type", "https://fiadoya.local/errors/" + code.toLowerCase().replace('_', '-'));
        cuerpo.put("title", titulo);
        cuerpo.put("status", status.value());
        cuerpo.put("detail", detalle);
        cuerpo.put("code", code);
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(MAPPER.writeValueAsBytes(cuerpo));
        return response.writeWith(Mono.just(buffer));
    }
}
