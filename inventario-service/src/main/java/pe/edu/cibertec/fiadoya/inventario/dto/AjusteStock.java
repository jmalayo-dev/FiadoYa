package pe.edu.cibertec.fiadoya.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AjusteStock(@NotNull Integer delta, @NotBlank @Size(min = 3, max = 120) String motivo) {
}
