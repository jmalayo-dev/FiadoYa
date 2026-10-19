package pe.edu.cibertec.fiadoya.reportes.client;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import pe.edu.cibertec.fiadoya.reportes.config.FeignInternoConfig;

/** Cliente Feign 3 (R10): reportes no tiene tabla de clientes; pide los deudores a clientes-fiado. */
@FeignClient(name = "clientes-fiado-service", configuration = FeignInternoConfig.class)
public interface ClientesClient {

    @GetMapping("/internal/clientes/deudores")
    List<Deudor> deudores();

    record Deudor(Long clienteId, String nombre, String telefono, BigDecimal saldo, BigDecimal limiteCredito) {
    }
}
