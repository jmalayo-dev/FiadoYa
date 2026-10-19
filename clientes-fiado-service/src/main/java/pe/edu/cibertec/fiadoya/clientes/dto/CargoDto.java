package pe.edu.cibertec.fiadoya.clientes.dto;

import java.math.BigDecimal;
import pe.edu.cibertec.fiadoya.clientes.entity.Cargo;

public record CargoDto(Long id, Long clienteId, Long ventaId, BigDecimal monto, boolean anulado) {

    public static CargoDto de(Cargo c) {
        return new CargoDto(c.getId(), c.getClienteId(), c.getVentaId(), c.getMonto(), c.isAnulado());
    }
}
