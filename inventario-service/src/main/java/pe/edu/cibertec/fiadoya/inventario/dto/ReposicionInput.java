package pe.edu.cibertec.fiadoya.inventario.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReposicionInput(@NotNull Long productoId, @NotNull @Min(1) @Max(9999) Integer cantidad) {
}
