package pe.edu.cibertec.fiadoya.inventario.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoDescuento(List<Item> items) {

    public record Item(Long productoId, String nombre, BigDecimal precioUnitario, int cantidad) {
    }
}
