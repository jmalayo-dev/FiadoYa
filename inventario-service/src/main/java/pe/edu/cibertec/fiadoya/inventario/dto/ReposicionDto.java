package pe.edu.cibertec.fiadoya.inventario.dto;

import java.time.LocalDate;

public record ReposicionDto(Long id, Long productoId, String productoNombre, int cantidad, String nroPedido,
                            LocalDate fechaEstimada, boolean recibida) {
}
