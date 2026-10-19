package pe.edu.cibertec.fiadoya.reportes.service;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.reportes.client.ClientesClient;

/** Pide los deudores a clientes-fiado por Feign, protegido con el circuito "clientes". */
@Component
public class DatosDeudores {

    private final ClientesClient clientes;
    private final CircuitBreakerFactory<?, ?> circuitos;

    public DatosDeudores(ClientesClient clientes, CircuitBreakerFactory<?, ?> circuitos) {
        this.clientes = clientes;
        this.circuitos = circuitos;
    }

    public List<ClientesClient.Deudor> obtener() {
        return circuitos.create("clientes").run(clientes::deudores, error -> {
            throw new ApiException(ErrorCode.SERVICIO_NO_DISPONIBLE, "No se pudo obtener la lista de deudores.",
                    Map.of("servicio", "clientes"));
        });
    }
}
