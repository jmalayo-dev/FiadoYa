package pe.edu.cibertec.fiadoya.ventas.client;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pe.edu.cibertec.fiadoya.ventas.config.FeignInternoConfig;

/** Cliente Feign 1 (síncrono): inventario-service, encontrado por Eureka. */
@FeignClient(name = "inventario-service", configuration = FeignInternoConfig.class)
public interface InventarioClient {

    @PostMapping("/internal/inventario/descuentos")
    Resultado descontar(@RequestBody Lote lote);

    @PostMapping("/internal/inventario/devoluciones")
    void devolver(@RequestBody Devolucion devolucion);

    record Lote(Long ventaId, List<Item> items) {
    }

    record Item(Long productoId, int cantidad) {
    }

    record Resultado(List<ItemPrecio> items) {
    }

    record ItemPrecio(Long productoId, String nombre, BigDecimal precioUnitario, int cantidad) {
    }

    record Devolucion(Long ventaId) {
    }
}
