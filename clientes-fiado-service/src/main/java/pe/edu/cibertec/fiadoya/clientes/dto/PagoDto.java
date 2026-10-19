package pe.edu.cibertec.fiadoya.clientes.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PagoDto(Long id, Long clienteId, BigDecimal monto, String metodo, Instant fecha, BigDecimal saldoDespues) {
}
