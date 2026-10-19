package pe.edu.cibertec.fiadoya.common.security;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad común de los servicios de negocio (R6): cada servicio es Resource Server JWT.
 * Las reglas por rol se declaran con @PreAuthorize en los controladores.
 */
@AutoConfiguration(beforeName = {
        "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
        "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration",
        "org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration",
        "org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration"})
@EnableConfigurationProperties(SeguridadProperties.class)
@EnableMethodSecurity
@Import(AccesoDenegadoAdvice.class)
public class SeguridadAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    JwtDecoder jwtDecoder(SeguridadProperties props) {
        return JwtSoporte.decodificador(props);
    }

    @Bean
    @ConditionalOnMissingBean
    SecurityFilterChain fiadoyaSecurityFilterChain(HttpSecurity http, SeguridadProperties props) throws Exception {
        http
                .csrf(c -> c.disable())
                .cors(c -> c.disable())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new InternalKeyFilter(props.internalKey()), BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(a -> a
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/ws/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/internal/**").hasRole(Roles.INTERNO)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(JwtSoporte.conversorRoles()))
                        .authenticationEntryPoint(RespuestasSeguridad.noAutenticado())
                        .accessDeniedHandler(RespuestasSeguridad.sinPermiso()))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(RespuestasSeguridad.noAutenticado())
                        .accessDeniedHandler(RespuestasSeguridad.sinPermiso()));
        return http.build();
    }
}
