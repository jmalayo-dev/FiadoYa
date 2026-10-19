package pe.edu.cibertec.fiadoya.ventas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemInput(@NotNull Long productoId, @NotNull @Min(1) @Max(999) Integer cantidad) {
}
