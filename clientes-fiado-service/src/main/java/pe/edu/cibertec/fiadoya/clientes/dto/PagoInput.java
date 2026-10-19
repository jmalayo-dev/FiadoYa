package pe.edu.cibertec.fiadoya.clientes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import pe.edu.cibertec.fiadoya.clientes.entity.MetodoPago;

public record PagoInput(
        @NotNull @DecimalMin(value = "0.01", message = "debe ser mayor que 0") @Digits(integer = 8, fraction = 2) BigDecimal monto,
        MetodoPago metodo) {
}
