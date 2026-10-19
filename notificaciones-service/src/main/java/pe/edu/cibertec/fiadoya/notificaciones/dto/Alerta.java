package pe.edu.cibertec.fiadoya.notificaciones.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import pe.edu.cibertec.fiadoya.contracts.ClienteLimiteCercano;
import pe.edu.cibertec.fiadoya.contracts.StockBajo;

/**
 * Alerta del contrato (Anexo §6.3 + v1.2 E4), unión por "tipo":
 * STOCK_BAJO lleva productoId, stock y stockMinimo; LIMITE_CLIENTE lleva clienteId, saldo y limiteCredito.
 * El id es el eventId para que el frontend deduplique.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Alerta(String id, String tipo, String nombre, Instant ocurridoEn,
                     Long productoId, Integer stock, Integer stockMinimo,
                     Long clienteId, BigDecimal saldo, BigDecimal limiteCredito) {

    public static final String STOCK_BAJO = "STOCK_BAJO";
    public static final String LIMITE_CLIENTE = "LIMITE_CLIENTE";

    public static Alerta de(StockBajo e) {
        return new Alerta(e.eventId().toString(), STOCK_BAJO, e.nombre(), e.ocurridoEn(),
                e.productoId(), e.stock(), e.stockMinimo(), null, null, null);
    }

    public static Alerta de(ClienteLimiteCercano e) {
        return new Alerta(e.eventId().toString(), LIMITE_CLIENTE, e.nombre(), e.ocurridoEn(),
                null, null, null, e.clienteId(), e.saldo(), e.limiteCredito());
    }
}
