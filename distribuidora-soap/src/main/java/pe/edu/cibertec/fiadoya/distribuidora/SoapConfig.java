package pe.edu.cibertec.fiadoya.distribuidora;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Publica el endpoint en /ws/distribuidora; el WSDL queda en /ws/distribuidora?wsdl. */
@Configuration
public class SoapConfig {

    @Bean
    Endpoint distribuidoraEndpoint(Bus bus, CatalogoDistribuidora catalogo) {
        EndpointImpl endpoint = new EndpointImpl(bus, new DistribuidoraEndpoint(catalogo));
        endpoint.publish("/distribuidora");
        return endpoint;
    }
}
