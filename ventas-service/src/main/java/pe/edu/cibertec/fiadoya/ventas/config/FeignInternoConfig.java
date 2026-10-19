package pe.edu.cibertec.fiadoya.ventas.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import pe.edu.cibertec.fiadoya.common.security.InternalKeyFilter;
import pe.edu.cibertec.fiadoya.common.security.SeguridadProperties;

/**
 * Configuración de los clientes Feign internos (R1):
 * agrega X-Internal-Key, traduce errores ProblemDetail y reintenta 2 veces solo fallos de red
 * (todos los /internal son idempotentes).
 */
public class FeignInternoConfig {

    @Bean
    RequestInterceptor claveInterna(SeguridadProperties props) {
        return plantilla -> plantilla.header(InternalKeyFilter.HEADER, props.internalKey());
    }

    @Bean
    ErrorDecoder problemDetailDecoder() {
        return new ProblemDetailErrorDecoder();
    }

    @Bean
    Retryer reintentos() {
        return new Retryer.Default(200, 1000, 3);
    }

    @Bean
    Logger.Level nivelLog() {
        return Logger.Level.BASIC;
    }
}
