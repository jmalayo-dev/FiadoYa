package pe.edu.cibertec.fiadoya.inventario.client;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.distribuidora.ws.DistribuidoraPortType;
import pe.edu.cibertec.fiadoya.distribuidora.ws.Pedido;
import pe.edu.cibertec.fiadoya.inventario.dto.CatalogoItem;

/**
 * Cliente JAX-WS generado desde el WSDL de la distribuidora (Flujo C).
 * Timeout de 5 s y circuit breaker "distribuidora": SOAP Fault, timeout o circuito abierto → 503.
 */
@Component
public class DistribuidoraClient {

    private final DistribuidoraPortType port;
    private final CircuitBreaker circuito;

    public DistribuidoraClient(@Value("${fiadoya.soap.url}") String url,
                               @Value("${fiadoya.soap.timeout-ms:5000}") long timeoutMs,
                               CircuitBreakerFactory<?, ?> circuitos) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(DistribuidoraPortType.class);
        factory.setAddress(url);
        this.port = (DistribuidoraPortType) factory.create();
        Client cliente = ClientProxy.getClient(port);
        HTTPClientPolicy politica = new HTTPClientPolicy();
        politica.setConnectionTimeout(timeoutMs);
        politica.setReceiveTimeout(timeoutMs);
        ((HTTPConduit) cliente.getConduit()).setClient(politica);
        this.circuito = circuitos.create("distribuidora");
    }

    public List<CatalogoItem> consultarCatalogo() {
        return circuito.run(() -> port.consultarCatalogo().stream()
                        .map(i -> new CatalogoItem(i.getCodigo(), i.getNombre(), i.getPrecio(), i.getDisponible()))
                        .toList(),
                DistribuidoraClient::noDisponible);
    }

    public PedidoRegistrado registrarPedido(String codigo, int cantidad) {
        return circuito.run(() -> {
            Pedido p = port.registrarPedido(codigo, cantidad);
            LocalDate fecha = p.getFechaEstimada().toGregorianCalendar().toZonedDateTime().toLocalDate();
            return new PedidoRegistrado(p.getNroPedido(), fecha);
        }, DistribuidoraClient::noDisponible);
    }

    private static <T> T noDisponible(Throwable causa) {
        throw new ApiException(pe.edu.cibertec.fiadoya.common.web.ErrorCode.SERVICIO_NO_DISPONIBLE,
                "La distribuidora no responde.", Map.of("servicio", "distribuidora"));
    }

    /** "P" + id con 4 dígitos (Anexo §9). */
    public static String codigoDe(Long productoId) {
        return "P%04d".formatted(productoId);
    }

    public record PedidoRegistrado(String nroPedido, LocalDate fechaEstimada) {
    }
}
