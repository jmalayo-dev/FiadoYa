package pe.edu.cibertec.fiadoya.inventario.dto;

import java.math.BigDecimal;
import pe.edu.cibertec.fiadoya.inventario.entity.Producto;

/** Producto del contrato (Anexo §6.3 + v1.2 E3). */
public record ProductoDto(Long id, String nombre, BigDecimal precio, BigDecimal costo, int stock, int stockMinimo,
                          String categoria, String codigoBarras, boolean activo) {

    public static ProductoDto de(Producto p) {
        return new ProductoDto(p.getId(), p.getNombre(), p.getPrecio(), p.getCosto(), p.getStock(), p.getStockMinimo(),
                p.getCategoria().name(), p.getCodigoBarras(), p.isActivo());
    }
}
