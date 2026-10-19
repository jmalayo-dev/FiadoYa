package pe.edu.cibertec.fiadoya.clientes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ClienteInput(
        @NotBlank @Size(min = 2, max = 80) String nombre,
        @NotBlank @Pattern(regexp = "^9\\d{8}$", message = "debe tener 9 dígitos y empezar en 9") String telefono,
        @NotNull @DecimalMin(value = "0.00", message = "debe ser >= 0") @Digits(integer = 8, fraction = 2) BigDecimal limiteCredito,
        @NotNull Boolean consentimiento,
        Long usuarioId,
        @Pattern(regexp = "^\\d{8}$", message = "debe tener 8 dígitos") String dni,
        @Size(max = 120) String direccion) {
}
