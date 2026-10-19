package pe.edu.cibertec.fiadoya.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Se publica cuando un cargo hace que el cliente cruce el 90 % de su límite (Anexo v1.2 E4). */
public record ClienteLimiteCercano(
        UUID eventId,
        Instant ocurridoEn,
        int version,
        String tipo,
        Long clienteId,
        String nombre,
        BigDecimal saldo,
        BigDecimal limiteCredito) implements Evento {

    public static final String TIPO = "ClienteLimiteCercano";

    public static ClienteLimiteCercano nuevo(Long clienteId, String nombre, BigDecimal saldo, BigDecimal limiteCredito) {
        return new ClienteLimiteCercano(UUID.randomUUID(), Instant.now(), 1, TIPO, clienteId, nombre, saldo, limiteCredito);
    }
}
