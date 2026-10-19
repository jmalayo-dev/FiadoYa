package pe.edu.cibertec.fiadoya.clientes.dto;

import java.math.BigDecimal;

public record Deudor(Long clienteId, String nombre, String telefono, BigDecimal saldo, BigDecimal limiteCredito) {
}
