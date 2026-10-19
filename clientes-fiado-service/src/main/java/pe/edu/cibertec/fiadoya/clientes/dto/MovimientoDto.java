package pe.edu.cibertec.fiadoya.clientes.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.edu.cibertec.fiadoya.clientes.entity.MovimientoCuenta;

public record MovimientoDto(String tipo, Instant fecha, BigDecimal monto, Long ventaId, BigDecimal saldoDespues,
                            String descripcion, Integer cantidadItems, String metodoPago) {

    public static MovimientoDto de(MovimientoCuenta m) {
        return new MovimientoDto(m.getTipo().name(), m.getFecha(), m.getMonto(), m.getVentaId(), m.getSaldoDespues(),
                m.getDescripcion(), m.getCantidadItems(), m.getMetodoPago() == null ? null : m.getMetodoPago().name());
    }
}
