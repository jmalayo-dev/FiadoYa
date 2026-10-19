package pe.edu.cibertec.fiadoya.clientes.dto;

import java.util.List;

public record EstadoCuenta(ClienteDto cliente, List<MovimientoDto> movimientos) {
}
