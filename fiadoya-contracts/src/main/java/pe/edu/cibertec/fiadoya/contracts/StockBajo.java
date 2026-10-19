package pe.edu.cibertec.fiadoya.contracts;

import java.time.Instant;
import java.util.UUID;

public record StockBajo(
        UUID eventId,
        Instant ocurridoEn,
        int version,
        String tipo,
        Long productoId,
        String nombre,
        int stock,
        int stockMinimo) implements Evento {

    public static final String TIPO = "StockBajo";

    public static StockBajo nuevo(Long productoId, String nombre, int stock, int stockMinimo) {
        return new StockBajo(UUID.randomUUID(), Instant.now(), 1, TIPO, productoId, nombre, stock, stockMinimo);
    }
}
