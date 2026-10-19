package pe.edu.cibertec.fiadoya.clientes.dto;

import java.math.BigDecimal;

public record ClienteResumen(long totalClientes, long clientesConDeuda, BigDecimal deudaTotal) {
}
