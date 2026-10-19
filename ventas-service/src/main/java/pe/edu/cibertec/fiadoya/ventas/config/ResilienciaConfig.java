package pe.edu.cibertec.fiadoya.ventas.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.edu.cibertec.fiadoya.common.web.ApiException;

/**
 * Circuitos "inventario" y "clientes" (Flujo A). Un 409 de negocio (ApiException) no es una falla técnica:
 * se ignora para que un cliente sin crédito no abra el circuito.
 */
@Configuration
public class ResilienciaConfig {

    @Bean
    Customizer<Resilience4JCircuitBreakerFactory> circuitos(
            @Value("${fiadoya.circuitos.ventana:10}") int ventana,
            @Value("${fiadoya.circuitos.minimo-llamadas:4}") int minimo,
            @Value("${fiadoya.circuitos.porcentaje-fallos:50}") float porcentaje,
            @Value("${fiadoya.circuitos.segundos-abierto:20}") int segundosAbierto) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(ventana)
                .minimumNumberOfCalls(minimo)
                .failureRateThreshold(porcentaje)
                .waitDurationInOpenState(Duration.ofSeconds(segundosAbierto))
                .permittedNumberOfCallsInHalfOpenState(2)
                .ignoreExceptions(ApiException.class)
                .build();
        return factory -> factory.configure(builder -> builder.circuitBreakerConfig(config), "inventario", "clientes");
    }
}
