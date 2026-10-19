package pe.edu.cibertec.fiadoya.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumenVentas(LocalDate desde, LocalDate hasta, long totalVentas, BigDecimal montoTotal,
                            List<PorDia> porDia, List<TopProducto> topProductos) {

    public record PorDia(LocalDate fecha, long cantidad, BigDecimal monto) {
    }

    public record TopProducto(Long productoId, String nombre, long cantidad, BigDecimal monto) {
    }
}
