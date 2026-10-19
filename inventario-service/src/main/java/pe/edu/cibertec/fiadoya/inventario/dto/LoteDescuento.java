package pe.edu.cibertec.fiadoya.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** POST /internal/inventario/descuentos (Anexo §10.1). */
public record LoteDescuento(@NotNull Long ventaId, @NotEmpty @Size(max = 50) List<@Valid Item> items) {

    public record Item(@NotNull Long productoId, @NotNull @Min(1) @Max(999) Integer cantidad) {
    }
}
