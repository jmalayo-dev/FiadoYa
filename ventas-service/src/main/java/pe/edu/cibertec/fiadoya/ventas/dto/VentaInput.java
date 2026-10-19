package pe.edu.cibertec.fiadoya.ventas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import pe.edu.cibertec.fiadoya.ventas.entity.TipoPago;

/** R3: no lleva precios; ventas los obtiene de inventario. */
public record VentaInput(Long clienteId, @NotNull TipoPago tipoPago,
                         @NotEmpty @Size(max = 50) List<@Valid @NotNull ItemInput> items) {
}
