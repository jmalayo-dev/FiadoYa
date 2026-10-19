package pe.edu.cibertec.fiadoya.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VentaRegistrada(
        UUID eventId,
        Instant ocurridoEn,
        int version,
        String tipo,
        Long ventaId,
        Long clienteId,
        String tipoPago,
        BigDecimal total,
        List<Item> items) implements Evento {

    public static final String TIPO = "VentaRegistrada";

    public record Item(Long productoId, String nombre, int cantidad, BigDecimal precioUnitario) {
    }

    public static VentaRegistrada nueva(Long ventaId, Long clienteId, String tipoPago, BigDecimal total, List<Item> items) {
        return new VentaRegistrada(UUID.randomUUID(), Instant.now(), 1, TIPO, ventaId, clienteId, tipoPago, total, List.copyOf(items));
    }
}
