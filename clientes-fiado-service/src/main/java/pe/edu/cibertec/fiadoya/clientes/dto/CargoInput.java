package pe.edu.cibertec.fiadoya.clientes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** POST /internal/clientes/{id}/cargos (Anexo §6.2 + v1.2 E6). */
public record CargoInput(@NotNull Long ventaId, @NotNull @DecimalMin("0.01") BigDecimal monto,
                         @Size(max = 160) String descripcion, Integer cantidadItems) {
}
