package pe.edu.cibertec.fiadoya.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.cibertec.fiadoya.auth.entity.Rol;

/** Anexo §3.1: username en minúsculas y único; contraseña de 8+ con letra y dígito. */
public record UsuarioInput(
        @NotBlank @Size(min = 3, max = 40)
        @Pattern(regexp = "^[a-z0-9._-]+$", message = "solo minúsculas, dígitos, punto, guion o guion bajo")
        String username,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe tener al menos una letra y un dígito")
        String password,
        @NotNull Rol rol) {
}
