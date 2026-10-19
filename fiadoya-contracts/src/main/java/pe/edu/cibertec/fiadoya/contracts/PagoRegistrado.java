package pe.edu.cibertec.fiadoya.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PagoRegistrado(
        UUID eventId,
        Instant ocurridoEn,
        int version,
        String tipo,
        Long pagoId,
        Long clienteId,
        BigDecimal monto,
        BigDecimal saldoDespues,
        String metodo) implements Evento {

    public static final String TIPO = "PagoRegistrado";

    public static PagoRegistrado nuevo(Long pagoId, Long clienteId, BigDecimal monto, BigDecimal saldoDespues, String metodo) {
        return new PagoRegistrado(UUID.randomUUID(), Instant.now(), 1, TIPO, pagoId, clienteId, monto, saldoDespues, metodo);
    }
}
