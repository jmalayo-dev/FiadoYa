package pe.edu.cibertec.fiadoya.clientes.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.edu.cibertec.fiadoya.clientes.entity.Cliente;

/** Cliente del contrato (Anexo §6.3 + v1.2 E1). disponible = límite - saldo. */
public record ClienteDto(Long id, Long usuarioId, String dni, String nombre, String telefono, String direccion,
                         BigDecimal limiteCredito, BigDecimal saldo, BigDecimal disponible, boolean consentimiento,
                         Instant fechaConsentimiento, boolean activo) {

    public static ClienteDto de(Cliente c) {
        return new ClienteDto(c.getId(), c.getUsuarioId(), c.getDni(), c.getNombre(), c.getTelefono(), c.getDireccion(),
                c.getLimiteCredito(), c.getSaldo(), c.disponible(), c.isConsentimiento(), c.getFechaConsentimiento(),
                c.isActivo());
    }
}
