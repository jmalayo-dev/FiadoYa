package pe.edu.cibertec.fiadoya.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import pe.edu.cibertec.fiadoya.inventario.entity.Categoria;

/** stock solo se usa al crear; en PUT se ignora (el stock cambia por ajustes, ventas y reposiciones). */
public record ProductoInput(
        @NotBlank @Size(min = 2, max = 80) String nombre,
        @NotNull @DecimalMin(value = "0.01", message = "debe ser mayor que 0") @Digits(integer = 8, fraction = 2) BigDecimal precio,
        @DecimalMin(value = "0.01", message = "debe ser mayor que 0") @Digits(integer = 8, fraction = 2) BigDecimal costo,
        @Min(value = 0, message = "debe ser >= 0") Integer stock,
        @NotNull @Min(value = 0, message = "debe ser >= 0") Integer stockMinimo,
        Categoria categoria,
        @Pattern(regexp = "^\\d{8,14}$", message = "debe tener de 8 a 14 dígitos") String codigoBarras) {
}
