package pe.edu.cibertec.fiadoya.inventario.dto;

import java.math.BigDecimal;

public record CatalogoItem(String codigo, String nombre, BigDecimal precio, int disponible) {
}
