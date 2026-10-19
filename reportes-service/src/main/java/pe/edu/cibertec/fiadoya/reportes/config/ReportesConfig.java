package pe.edu.cibertec.fiadoya.reportes.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportesConfig {

    /** Pool de hilos para generar reportes en paralelo (concurrencia, sílabo 5401). */
    @Bean(destroyMethod = "shutdown")
    ExecutorService ejecutorReportes(@Value("${fiadoya.reportes.hilos:3}") int hilos) {
        AtomicInteger n = new AtomicInteger();
        return Executors.newFixedThreadPool(hilos, r -> {
            Thread t = new Thread(r, "reporte-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    @Bean
    Customizer<Resilience4JCircuitBreakerFactory> circuitoClientes(
            @Value("${fiadoya.circuitos.ventana:10}") int ventana,
            @Value("${fiadoya.circuitos.minimo-llamadas:4}") int minimo,
            @Value("${fiadoya.circuitos.porcentaje-fallos:50}") float porcentaje,
            @Value("${fiadoya.circuitos.segundos-abierto:20}") int segundosAbierto) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(ventana).minimumNumberOfCalls(minimo).failureRateThreshold(porcentaje)
                .waitDurationInOpenState(Duration.ofSeconds(segundosAbierto)).permittedNumberOfCallsInHalfOpenState(2)
                .build();
        return factory -> factory.configure(builder -> builder.circuitBreakerConfig(config), "clientes");
    }
}
