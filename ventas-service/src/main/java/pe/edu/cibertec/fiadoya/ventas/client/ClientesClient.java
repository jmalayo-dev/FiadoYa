package pe.edu.cibertec.fiadoya.ventas.client;

import java.math.BigDecimal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pe.edu.cibertec.fiadoya.ventas.config.FeignInternoConfig;

/** Cliente Feign 2 (síncrono): clientes-fiado-service. */
@FeignClient(name = "clientes-fiado-service", configuration = FeignInternoConfig.class)
public interface ClientesClient {

    @GetMapping("/internal/clientes/{id}/validacion")
    Validacion validar(@PathVariable("id") Long clienteId);

    @PostMapping("/internal/clientes/{id}/cargos")
    Cargo cargar(@PathVariable("id") Long clienteId, @RequestBody CargoInput cargo);

    @DeleteMapping("/internal/clientes/cargos/{ventaId}")
    void anular(@PathVariable("ventaId") Long ventaId);

    record Validacion(boolean existe, boolean activo, boolean consentimiento, String nombre) {
    }

    record CargoInput(Long ventaId, BigDecimal monto, String descripcion, Integer cantidadItems) {
    }

    record Cargo(Long id, Long clienteId, Long ventaId, BigDecimal monto, boolean anulado) {
    }
}
