package pe.edu.cibertec.fiadoya.ventas.dto;

import java.math.BigDecimal;

public record DetalleVentaDto(Long productoId, String nombre, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
}
