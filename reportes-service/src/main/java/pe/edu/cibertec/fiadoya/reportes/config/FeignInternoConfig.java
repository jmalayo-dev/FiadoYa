package pe.edu.cibertec.fiadoya.reportes.config;

import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import pe.edu.cibertec.fiadoya.common.security.InternalKeyFilter;
import pe.edu.cibertec.fiadoya.common.security.SeguridadProperties;

/** Agrega X-Internal-Key y reintenta 2 veces los fallos de red (GET idempotente). */
public class FeignInternoConfig {

    @Bean
    RequestInterceptor claveInterna(SeguridadProperties props) {
        return plantilla -> plantilla.header(InternalKeyFilter.HEADER, props.internalKey());
    }

    @Bean
    Retryer reintentos() {
        return new Retryer.Default(200, 1000, 3);
    }
}
