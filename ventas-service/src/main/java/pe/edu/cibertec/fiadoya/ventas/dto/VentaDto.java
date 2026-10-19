package pe.edu.cibertec.fiadoya.ventas.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import pe.edu.cibertec.fiadoya.ventas.entity.Venta;

public record VentaDto(Long id, Long clienteId, String clienteNombre, String tipoPago, String estado, BigDecimal total,
                       String motivoRechazo, Instant fecha, List<DetalleVentaDto> items) {

    public static VentaDto de(Venta v) {
        return new VentaDto(v.getId(), v.getClienteId(), v.getClienteNombre(), v.getTipoPago().name(), v.getEstado().name(),
                v.getTotal(), v.getMotivoRechazo(), v.getFecha(),
                v.getItems().stream().map(d -> new DetalleVentaDto(d.getProductoId(), d.getNombreProducto(), d.getCantidad(),
                        d.getPrecioUnitario(), d.subtotal())).toList());
    }
}
